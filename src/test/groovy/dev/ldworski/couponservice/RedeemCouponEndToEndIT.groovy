package dev.ldworski.couponservice

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

import com.github.tomakehurst.wiremock.WireMockServer
import dev.ldworski.couponservice.configuration.TestcontainersConfiguration
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration)
@AutoConfigureTracing
class RedeemCouponEndToEndIT extends IntegrationTestSpecification {

    private static final String POLISH_IP = "83.0.0.1"
    private static final Duration LOCK_ACQUISITION_WAIT = Duration.ofSeconds(5)
    private static final Duration REQUEST_OVERHEAD_TOLERANCE = Duration.ofSeconds(1)
    private static final Duration LOCK_HOLD_LIMIT_BEYOND_TIMEOUT = Duration.ofSeconds(10)
    private static final Duration LOCK_HOLDER_FINISH_WAIT = Duration.ofSeconds(10)

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

    @Autowired
    PlatformTransactionManager transactionManager

    @Value('${spring.jpa.properties.jakarta.persistence.lock.timeout}')
    long lockTimeoutMillis

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

    def "should return generated trace id as correlation id in problem details"() {
        when:
        def response = redeemCoupon("E2EREDEEMMISSING", UUID.randomUUID())

        then:
        response.expectStatus().isNotFound()
                .expectBody().jsonPath('$.correlationId').isNotEmpty()
    }

    def "should return trace id from traceparent header as correlation id in problem details"() {
        when:
        def response = restTestClient.post()
                .uri("/coupons/E2EREDEEMMISSING/redemptions")
                .header("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"userId": "${UUID.randomUUID()}", "ipAddress": "$POLISH_IP"}""".toString())
                .exchange()

        then:
        response.expectStatus().isNotFound()
                .expectBody().jsonPath('$.correlationId').isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736")
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

    def "should redeem coupon only once when the same user redeems concurrently"() {
        given:
        registerCoupon("E2EREDEEMSAMEUSER", 10)
        def userId = UUID.randomUUID()
        def executor = Executors.newFixedThreadPool(10)
        def ready = new CountDownLatch(10)
        def start = new CountDownLatch(1)

        when:
        def statuses = (1..10).collect {
            executor.submit({
                ready.countDown()
                start.await()
                redeemCoupon("E2EREDEEMSAMEUSER", userId).returnResult().status.value()
            } as Callable<Integer>)
        }
        ready.await()
        start.countDown()
        def results = statuses.collect { future -> future.get(30, TimeUnit.SECONDS) }

        then:
        results.count { it == 201 } == 1
        results.count { it == 409 } == 9
        currentUsage("E2EREDEEMSAMEUSER") == 1
        redemptionCount("E2EREDEEMSAMEUSER") == 1

        cleanup:
        executor.shutdownNow()
    }

    def "should return service unavailable when coupon lock is not acquired within lock timeout"() {
        given:
        registerCoupon("E2ELOCKTIMEOUT", 10)
        def locked = new CountDownLatch(1)
        def release = new CountDownLatch(1)
        def executor = Executors.newSingleThreadExecutor()
        def lockHolder = executor.submit {
            new TransactionTemplate(transactionManager).executeWithoutResult {
                jdbcTemplate.queryForObject("SELECT id FROM coupon WHERE code = ? FOR UPDATE", UUID, "E2ELOCKTIMEOUT")
                locked.countDown()
                release.await(lockTimeout().plus(LOCK_HOLD_LIMIT_BEYOND_TIMEOUT).toMillis(), TimeUnit.MILLISECONDS)
            }
        }
        assert locked.await(LOCK_ACQUISITION_WAIT.toMillis(), TimeUnit.MILLISECONDS)

        when:
        def start = System.nanoTime()
        def response = redeemCoupon("E2ELOCKTIMEOUT", UUID.randomUUID())
        def elapsed = Duration.ofNanos(System.nanoTime() - start)

        then:
        response.expectStatus().isEqualTo(503)
                .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "1")
                .expectBody().jsonPath('$.title').isEqualTo("Coupon temporarily unavailable")
        elapsed >= lockTimeout()
        elapsed < lockTimeout().plus(REQUEST_OVERHEAD_TOLERANCE)

        when:
        release.countDown()
        lockHolder.get(LOCK_HOLDER_FINISH_WAIT.toMillis(), TimeUnit.MILLISECONDS)

        then:
        currentUsage("E2ELOCKTIMEOUT") == 0
        redemptionCount("E2ELOCKTIMEOUT") == 0

        cleanup:
        release.countDown()
        executor.shutdownNow()
    }

    private Duration lockTimeout() {
        Duration.ofMillis(lockTimeoutMillis)
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
