package dev.ldworski.couponservice.adapter.in.web

import dev.ldworski.couponservice.domain.exception.CouponAlreadyRedeemedException
import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInLocationException
import dev.ldworski.couponservice.domain.exception.CouponNotFoundException
import dev.ldworski.couponservice.domain.exception.CouponUsageLimitReachedException
import dev.ldworski.couponservice.domain.model.CouponCode
import dev.ldworski.couponservice.domain.model.CouponId
import dev.ldworski.couponservice.domain.model.IpAddress
import dev.ldworski.couponservice.domain.model.UserId
import dev.ldworski.couponservice.domain.port.in.RedeemCouponUseCase
import dev.ldworski.couponservice.domain.port.in.RedeemCouponUseCase.RedeemCouponCommand
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CouponRedemptionsController)
class CouponRedemptionsControllerIT extends Specification {

    private static final String CODE = "WINTER2026"
    private static final String USER_UUID = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    private static final UserId USER_ID = new UserId(UUID.fromString(USER_UUID))
    private static final String VALID_BODY = """{"userId": "$USER_UUID", "ipAddress": "83.0.0.1"}"""

    @Autowired
    MockMvc mockMvc

    @SpringBean
    RedeemCouponUseCase redeemCouponUseCase = Mock()

    def "should redeem coupon"() {
        when:
        def result = mockMvc.perform(post("/coupons/{code}/redemptions", CODE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))

        then:
        1 * redeemCouponUseCase.redeemCoupon(
                new RedeemCouponCommand(new CouponCode(CODE), USER_ID, new IpAddress("83.0.0.1")))

        and:
        result.andExpect(status().isCreated())
                .andExpect(content().string(""))
    }

    def "should redeem coupon for IPv6 address"() {
        when:
        def result = mockMvc.perform(post("/coupons/{code}/redemptions", CODE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId": "$USER_UUID", "ipAddress": "2a02:8108::1"}"""))

        then:
        1 * redeemCouponUseCase.redeemCoupon({ it.ipAddress() == new IpAddress("2a02:8108::1") })

        and:
        result.andExpect(status().isCreated())
    }

    def "should reject request when #description"() {
        when:
        def result = mockMvc.perform(post("/coupons/{code}/redemptions", code)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))

        then:
        0 * redeemCouponUseCase._

        and:
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath('$.status').value(400))
                .andExpect(jsonPath('$.title').value("Validation error"))
                .andExpect(jsonPath('$.detail').value("Request validation failed"))
                .andExpect(jsonPath('$.errors[*].field').value(everyItem(is(field))))
                .andExpect(jsonPath('$.errors[*].message').value(hasItem(message)))

        where:
        description                  | code          | requestBody                                                       || field       | message
        "code is lower case"         | "winter2026"  | VALID_BODY                                                        || "code"      | "must contain only uppercase letters A-Z or digits"
        "code contains dash"         | "WINTER-2026" | VALID_BODY                                                        || "code"      | "must contain only uppercase letters A-Z or digits"
        "code contains whitespace"   | "WINTER 2026" | VALID_BODY                                                        || "code"      | "must contain only uppercase letters A-Z or digits"
        "code is too long"           | "A" * 256     | VALID_BODY                                                        || "code"      | "size must be between 0 and 255"
        "user id is missing"         | CODE          | '{"ipAddress": "83.0.0.1"}'                                       || "userId"    | "must not be null"
        "user id is not UUID"        | CODE          | '{"userId": "user-1", "ipAddress": "83.0.0.1"}'                   || "userId"    | "is missing or has invalid value"
        "ip address is missing"      | CODE          | """{"userId": "$USER_UUID"}"""                                    || "ipAddress" | "must not be null"
        "ip address is empty"        | CODE          | """{"userId": "$USER_UUID", "ipAddress": ""}"""                   || "ipAddress" | "must be a valid IPv4 or IPv6 address"
        "ip address is a hostname"   | CODE          | """{"userId": "$USER_UUID", "ipAddress": "localhost"}"""          || "ipAddress" | "must be a valid IPv4 or IPv6 address"
        "ip address is out of range" | CODE          | """{"userId": "$USER_UUID", "ipAddress": "256.1.1.1"}"""          || "ipAddress" | "must be a valid IPv4 or IPv6 address"
        "ip address has port"        | CODE          | """{"userId": "$USER_UUID", "ipAddress": "83.0.0.1:8080"}"""      || "ipAddress" | "must be a valid IPv4 or IPv6 address"
        "ip address is a list"       | CODE          | """{"userId": "$USER_UUID", "ipAddress": "83.0.0.1, 10.0.0.1"}""" || "ipAddress" | "must be a valid IPv4 or IPv6 address"
    }

    def "should return all validation errors from path and body at once"() {
        when:
        def result = mockMvc.perform(post("/coupons/{code}/redemptions", "winter2026")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"ipAddress": "localhost"}'))

        then:
        0 * redeemCouponUseCase._

        and:
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.errors', hasSize(3)))
                .andExpect(jsonPath('$.errors[*].field').value(hasItem("code")))
                .andExpect(jsonPath('$.errors[*].field').value(hasItem("userId")))
                .andExpect(jsonPath('$.errors[*].field').value(hasItem("ipAddress")))
    }

    def "should reject malformed json"() {
        when:
        def result = mockMvc.perform(post("/coupons/{code}/redemptions", CODE)
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"userId": '))

        then:
        0 * redeemCouponUseCase._

        and:
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.title').value("Malformed request"))
    }

    def "should return #expectedStatus when #description"() {
        given:
        redeemCouponUseCase.redeemCoupon(_) >> { throw exception }

        when:
        def result = mockMvc.perform(post("/coupons/{code}/redemptions", CODE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))

        then:
        result.andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath('$.status').value(expectedStatus))
                .andExpect(jsonPath('$.title').value(expectedTitle))
                .andExpect(jsonPath('$.detail').value(expectedDetail))
                .andExpect(jsonPath('$.instance').value("/coupons/WINTER2026/redemptions"))

        where:
        description                          | exception                                                                 || expectedStatus | expectedTitle                     | expectedDetail
        "coupon does not exist"              | new CouponNotFoundException(new CouponCode(CODE))                         || 404            | "Coupon not found"                | "Coupon with code WINTER2026 not found"
        "coupon is not available in country" | new CouponNotAvailableInLocationException(new CouponCode(CODE))           || 403            | "Coupon not available in country" | "Coupon with code WINTER2026 is not available for user's location"
        "user has already redeemed coupon"   | new CouponAlreadyRedeemedException(CouponId.newCouponId(), USER_ID, null) || 409            | "Coupon already redeemed"         | "Coupon has already been redeemed by this user"
        "coupon usage limit is reached"      | new CouponUsageLimitReachedException(new CouponCode(CODE))                || 409            | "Coupon usage limit reached"      | "Coupon with code WINTER2026 has reached its usage limit"
    }

    def "should return coupon code in problem details when #description"() {
        given:
        redeemCouponUseCase.redeemCoupon(_) >> { throw exception }

        when:
        def result = mockMvc.perform(post("/coupons/{code}/redemptions", CODE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))

        then:
        result.andExpect(jsonPath('$.code').value(CODE))

        where:
        description                           | exception
        "coupon does not exist"               | new CouponNotFoundException(new CouponCode(CODE))
        "coupon is not available in location" | new CouponNotAvailableInLocationException(new CouponCode(CODE))
        "coupon usage limit is reached"       | new CouponUsageLimitReachedException(new CouponCode(CODE))
    }

    def "should return internal server error without exposing details when unexpected exception occurs"() {
        given:
        redeemCouponUseCase.redeemCoupon(_) >> { throw new IllegalStateException("database password leaked") }

        when:
        def result = mockMvc.perform(post("/coupons/{code}/redemptions", CODE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))

        then:
        result.andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath('$.detail').value("Unexpected error occurred"))
                .andExpect(jsonPath('$.instance').value("/coupons/WINTER2026/redemptions"))
    }
}
