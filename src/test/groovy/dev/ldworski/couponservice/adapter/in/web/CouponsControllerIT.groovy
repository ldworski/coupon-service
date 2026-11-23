package dev.ldworski.couponservice.adapter.in.web

import dev.ldworski.couponservice.domain.exception.CouponCodeAlreadyExistsException
import dev.ldworski.couponservice.domain.model.CountryCode
import dev.ldworski.couponservice.domain.model.CouponCode
import dev.ldworski.couponservice.domain.model.CouponDefinition
import dev.ldworski.couponservice.domain.model.CouponId
import dev.ldworski.couponservice.domain.port.in.RegisterCouponUseCase
import dev.ldworski.couponservice.domain.port.in.RegisterCouponUseCase.RegisterCouponCodeCommand
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import dev.ldworski.couponservice.IntegrationTestSpecification

import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CouponsController)
class CouponsControllerIT extends IntegrationTestSpecification {

    private static final CouponId COUPON_ID = new CouponId(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"))
    private static final int MAX_ALLOWED_USAGES = 100
    private static final CountryCode COUNTRY_CODE = CountryCode.PL

    @Autowired
    MockMvc mockMvc

    @SpringBean
    RegisterCouponUseCase registerCouponUseCase = Mock()

    def "should register coupon and return its id"() {
        when:
        def result = mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code": "WINTER2026",
                     "maxUsagesAllowed": ${MAX_ALLOWED_USAGES},
                     "countryCode": "${COUNTRY_CODE.name()}"}
                     """))

        then:
        1 * registerCouponUseCase.registerCoupon(new RegisterCouponCodeCommand(
                new CouponDefinition(new CouponCode("WINTER2026"), MAX_ALLOWED_USAGES, COUNTRY_CODE))) >> COUPON_ID

        and:
        result.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$.couponId').value(COUPON_ID.value().toString()))
    }

    def "should reject request when #description"() {
        when:
        def result = mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))

        then:
        0 * registerCouponUseCase._

        and:
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath('$.status').value(400))
                .andExpect(jsonPath('$.title').value("Validation error"))
                .andExpect(jsonPath('$.detail').value("Request validation failed"))
                .andExpect(jsonPath('$.instance').value("/coupons"))
                .andExpect(jsonPath('$.errors[*].field').value(everyItem(is(field))))

        where:
        description                          | requestBody                                                              || field
        "code is missing"                    | '{"maxUsagesAllowed": 100, "countryCode": "PL"}'                         || "code"
        "code is blank"                      | '{"code": "   ", "maxUsagesAllowed": 100, "countryCode": "PL"}'          || "code"
        "code is too long"                   | """{"code": "${'A' * 256}", "maxUsagesAllowed": 100, "countryCode": "PL"}""" || "code"
        "code contains at sign"              | '{"code": "WINTER@2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'  || "code"
        "code contains dot"                  | '{"code": "WINTER.2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'  || "code"
        "code contains inner whitespace"     | '{"code": "WINTER 2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'  || "code"
        "code has leading whitespace"        | '{"code": " WINTER2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'  || "code"
        "code has trailing whitespace"       | '{"code": "WINTER2026 ", "maxUsagesAllowed": 100, "countryCode": "PL"}'  || "code"
        "code has surrounding tabs"          | '{"code": "\\tWINTER2026\\n", "maxUsagesAllowed": 100, "countryCode": "PL"}' || "code"
        "code contains inner tab"            | '{"code": "WINTER\\t2026", "maxUsagesAllowed": 100, "countryCode": "PL"}' || "code"
        "code contains inner new line"       | '{"code": "WINTER\\n2026", "maxUsagesAllowed": 100, "countryCode": "PL"}' || "code"
        "code contains carriage return"      | '{"code": "WINTER\\r\\n2026", "maxUsagesAllowed": 100, "countryCode": "PL"}' || "code"
        "code contains non-breaking space"   | '{"code": "WINTER\\u00A02026", "maxUsagesAllowed": 100, "countryCode": "PL"}' || "code"
        "code contains em space"             | '{"code": "WINTER\\u20032026", "maxUsagesAllowed": 100, "countryCode": "PL"}' || "code"
        "code contains dash"                 | '{"code": "WINTER-2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'  || "code"
        "code contains underscore"           | '{"code": "WINTER_2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'  || "code"
        "code contains hash"                 | '{"code": "WINTER#2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'  || "code"
        "code contains Cyrillic letters"     | '{"code": "ЗИМА2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'     || "code"
        "code contains Cyrillic look-alike"  | '{"code": "ZIMА2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'     || "code"
        "code contains Greek letters"        | '{"code": "ΩMEGA2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'    || "code"
        "code is lower case"                 | '{"code": "winter2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'   || "code"
        "code is mixed case"                 | '{"code": "Winter2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'   || "code"
        "code contains lower case Polish letter" | '{"code": "ZIMAś2026", "maxUsagesAllowed": 100, "countryCode": "PL"}' || "code"
        "code contains Polish letter"        | '{"code": "ZIMAŚ2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'    || "code"
        "code contains Latin letter with diacritic" | '{"code": "ŽLUTÝ", "maxUsagesAllowed": 100, "countryCode": "PL"}' || "code"
        "code contains sharp s"              | '{"code": "STRAßE", "maxUsagesAllowed": 100, "countryCode": "PL"}'       || "code"
        "code contains non-ASCII digit"      | '{"code": "ZIMA٢٠٢٦", "maxUsagesAllowed": 100, "countryCode": "PL"}'     || "code"
        "max usages allowed is zero"         | '{"code": "WINTER2026", "maxUsagesAllowed": 0, "countryCode": "PL"}'     || "maxUsagesAllowed"
        "country code is missing"            | '{"code": "WINTER2026", "maxUsagesAllowed": 100}'                        || "countryCode"
        "max usages allowed is missing"      | '{"code": "WINTER2026", "countryCode": "PL"}'                            || "maxUsagesAllowed"
        "max usages allowed is not a number" | '{"code": "WINTER2026", "maxUsagesAllowed": "abc", "countryCode": "PL"}' || "maxUsagesAllowed"
        "country code is unknown"            | '{"code": "WINTER2026", "maxUsagesAllowed": 100, "countryCode": "XX"}'   || "countryCode"
    }

    def "should accept code with maximum length"() {
        given:
        def code = 'A' * 255

        when:
        def result = mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code": "${code}", "maxUsagesAllowed": 100, "countryCode": "PL"}"""))

        then:
        1 * registerCouponUseCase.registerCoupon({ it.couponDefinition().code() == new CouponCode(code) }) >> COUPON_ID

        and:
        result.andExpect(status().isCreated())
    }

    def "should accept code '#code' with allowed characters"() {
        when:
        def result = mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code": "${code}", "maxUsagesAllowed": 100, "countryCode": "PL"}"""))

        then:
        1 * registerCouponUseCase.registerCoupon({ it.couponDefinition().code() == new CouponCode(code) }) >> COUPON_ID

        and:
        result.andExpect(status().isCreated())

        where:
        code << ["WINTER2026", "2026WINTER", "A", "7", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"]
    }

    def "should report single readable error when code is '#code'"() {
        when:
        def result = mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code": "${code}", "maxUsagesAllowed": 100, "countryCode": "PL"}"""))

        then:
        0 * registerCouponUseCase._

        and:
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.errors', hasSize(1)))
                .andExpect(jsonPath('$.errors[0].field').value("code"))
                .andExpect(jsonPath('$.errors[0].message').value("must contain only uppercase letters A-Z or digits"))

        where:
        code << ["WINTER\$2026", "WINTER 2026", "Winter2026", "ZIMAŚ2026", "ЗИМА2026"]
    }

    def "should report unrecognized value when country code is unknown"() {
        when:
        def result = mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"code": "WINTER2026", "maxUsagesAllowed": 100, "countryCode": "XX"}'))

        then:
        0 * registerCouponUseCase._

        and:
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.errors', hasSize(1)))
                .andExpect(jsonPath('$.errors[0].field').value("countryCode"))
                .andExpect(jsonPath('$.errors[0].message').value("unrecognized value"))
    }

    def "should reject malformed json"() {
        when:
        def result = mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"code": "WINTER2026",'))

        then:
        0 * registerCouponUseCase._

        and:
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath('$.status').value(400))
                .andExpect(jsonPath('$.title').value("Malformed request"))
                .andExpect(jsonPath('$.instance').value("/coupons"))
    }

    def "should keep spring status when #description"() {
        when:
        def result = mockMvc.perform(request)

        then:
        0 * registerCouponUseCase._

        and:
        result.andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))

        where:
        description                    | request                                                                                                                                                                    || expectedStatus
        "http method is not supported" | delete("/coupons")                                                                                                                                                         || 405
        "content type is not supported"  | post("/coupons").contentType(MediaType.TEXT_PLAIN).content("x")                                                                                                            || 415
        "requested response type is not acceptable"  | post("/coupons").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_XML).content('{"code": "WINTER2026", "maxUsagesAllowed": 100, "countryCode": "PL"}') || 406
    }

    def "should return conflict when coupon code already exists"() {
        given:
        registerCouponUseCase.registerCoupon(_) >> {
            throw new CouponCodeAlreadyExistsException(new CouponCode("WINTER2026"), null)
        }

        when:
        def result = mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"code": "WINTER2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'))

        then:
        result.andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath('$.status').value(409))
                .andExpect(jsonPath('$.title').value("Coupon code already exists"))
                .andExpect(jsonPath('$.detail').value("Coupon with code WINTER2026 already exists"))
                .andExpect(jsonPath('$.code').value("WINTER2026"))
                .andExpect(jsonPath('$.instance').value("/coupons"))
    }

    def "should return internal server error without exposing details when unexpected exception occurs"() {
        given:
        registerCouponUseCase.registerCoupon(_) >> { throw new IllegalStateException("database password leaked") }

        when:
        def result = mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"code": "WINTER2026", "maxUsagesAllowed": 100, "countryCode": "PL"}'))

        then:
        result.andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath('$.status').value(500))
                .andExpect(jsonPath('$.title').value("Internal server error"))
                .andExpect(jsonPath('$.detail').value("Unexpected error occurred"))
                .andExpect(jsonPath('$.instance').value("/coupons"))
    }

    def "should return all validation errors at once"() {
        when:
        def result = mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"code": "", "maxUsagesAllowed": 0}'))

        then:
        0 * registerCouponUseCase._

        and:
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.errors', hasSize(3)))
    }
}
