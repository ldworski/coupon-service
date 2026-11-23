package dev.ldworski.couponservice

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

import com.github.tomakehurst.wiremock.WireMockServer
import dev.ldworski.couponservice.configuration.TestcontainersConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.client.RestTestClient

import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration)
class RedeemCouponEndToEndIT extends IntegrationTestSpecification {

    private static final String POLISH_IP = "83.0.0.1"

    static final WireMockServer wireMock = new WireMockServer(options().dynamicPort())

    static {
        wireMock.start()
    }

    @DynamicPropertySource
    static void freeIpApiProperties(DynamicPropertyRegistry registry) {
        registry.add("coupon-service.free-ip-api.base-url", wireMock::baseUrl)
    }

    @LocalServerPort
    int port

    @Autowired
    JdbcTemplate jdbcTemplate

    RestTestClient restTestClient

    def setup() {
        restTestClient = RestTestClient.bindToServer()
                .baseUrl("http://localhost:${port}")
                .build()
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/json/$POLISH_IP")).willReturn(okJson(
                new ClassPathResource("free-ip-api/identified-pl-ipv4.json").getContentAsString(StandardCharsets.UTF_8))))
    }

    def cleanupSpec() {
        wireMock.stop()
    }

    def "should redeem coupon"() {
        given:
        registerCoupon("E2EREDEEM", 10)
        def userId = UUID.randomUUID()

        when:
        def response = redeemCoupon("E2EREDEEM", userId)

        then:
        response.expectStatus().isCreated()
        currentUsage("E2EREDEEM") == 1
        redemptionCount("E2EREDEEM") == 1
    }

    def "should not increment coupon usage when user redeems coupon again"() {
        given:
        registerCoupon("E2EREDEEMTWICE", 10)
        def userId = UUID.randomUUID()
        redeemCoupon("E2EREDEEMTWICE", userId).expectStatus().isCreated()

        when:
        def response = redeemCoupon("E2EREDEEMTWICE", userId)

        then:
        response.expectStatus().isEqualTo(409)
                .expectBody().jsonPath('$.title').isEqualTo("Coupon already redeemed")
        currentUsage("E2EREDEEMTWICE") == 1
        redemptionCount("E2EREDEEMTWICE") == 1
    }

    def "should reject redemption when coupon usage limit is reached"() {
        given:
        registerCoupon("E2EREDEEMLIMIT", 1)
        redeemCoupon("E2EREDEEMLIMIT", UUID.randomUUID()).expectStatus().isCreated()

        when:
        def response = redeemCoupon("E2EREDEEMLIMIT", UUID.randomUUID())

        then:
        response.expectStatus().isEqualTo(409)
                .expectBody().jsonPath('$.title').isEqualTo("Coupon usage limit reached")
        currentUsage("E2EREDEEMLIMIT") == 1
        redemptionCount("E2EREDEEMLIMIT") == 1
    }

    def "should return not found when coupon does not exist"() {
        when:
        def response = redeemCoupon("E2EREDEEMMISSING", UUID.randomUUID())

        then:
        response.expectStatus().isNotFound()
                .expectBody().jsonPath('$.title').isEqualTo("Coupon not found")
    }

    def "should return unprocessable content when coupon is not available in user's location"() {
        given:
        registerCoupon("E2EREDEEMCOUNTRY", 10, "DE")

        when:
        def response = redeemCoupon("E2EREDEEMCOUNTRY", UUID.randomUUID())

        then:
        response.expectStatus().isEqualTo(422)
                .expectBody().jsonPath('$.title').isEqualTo("Coupon not available in location")
        currentUsage("E2EREDEEMCOUNTRY") == 0
        redemptionCount("E2EREDEEMCOUNTRY") == 0
    }

    def "should not exceed coupon usage limit when redeeming concurrently"() {
        given:
        registerCoupon("E2EREDEEMCONCURRENT", 3)
        def executor = Executors.newFixedThreadPool(10)
        def ready = new CountDownLatch(10)
        def start = new CountDownLatch(1)

        when:
        def statuses = (1..10).collect {
            executor.submit({
                ready.countDown()
                start.await()
                redeemCoupon("E2EREDEEMCONCURRENT", UUID.randomUUID()).returnResult().status.value()
            } as Callable<Integer>)
        }
        ready.await()
        start.countDown()
        def results = statuses.collect { future -> future.get(30, TimeUnit.SECONDS) }

        then:
        results.count { it == 201 } == 3
        results.count { it == 409 } == 7
        currentUsage("E2EREDEEMCONCURRENT") == 3
        redemptionCount("E2EREDEEMCONCURRENT") == 3

        cleanup:
        executor.shutdownNow()
    }

    private void registerCoupon(String code, int maxUsagesAllowed, String countryCode = "PL") {
        restTestClient.post()
                .uri("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"code": "$code", "maxUsagesAllowed": $maxUsagesAllowed, "countryCode": "$countryCode"}""".toString())
                .exchange()
                .expectStatus().isCreated()
    }

    private RestTestClient.ResponseSpec redeemCoupon(String code, UUID userId) {
        restTestClient.post()
                .uri("/coupons/$code/redemptions")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"userId": "$userId", "ipAddress": "$POLISH_IP"}""".toString())
                .exchange()
    }

    private int currentUsage(String code) {
        jdbcTemplate.queryForObject("SELECT current_usage FROM coupon WHERE code = ?", Integer, code)
    }

    private int redemptionCount(String code) {
        jdbcTemplate.queryForObject("""
                SELECT count(*) FROM coupon_redemption r JOIN coupon c ON c.id = r.coupon_id
                WHERE c.code = ?
                """, Integer, code)
    }
}
