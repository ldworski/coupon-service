package dev.ldworski.couponservice

import dev.ldworski.couponservice.configuration.TestcontainersConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration)
class RegisterCouponEndToEndIT extends Specification {

    private static final String UUID_PATTERN = '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'

    @LocalServerPort
    int port

    def "should register coupon"() {
        given:
        def restTestClient = RestTestClient.bindToServer()
                .baseUrl("http://localhost:${port}")
                .build()

        when:
        def response = restTestClient.post()
                .uri("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body('{"code": "e2e2026", "maxUsagesAllowed": 10, "countryCode": "PL"}')
                .exchange()

        then:
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.couponId').exists()
    }
}
