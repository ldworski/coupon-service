package dev.ldworski.couponservice.domain.model

import spock.lang.Specification

class CouponDefinitionSpec extends Specification {

    private static final CouponCode CODE = new CouponCode("WINTER2026")
    private static final CountryCode COUNTRY_CODE = CountryCode.PL

    def "should create definition with given values"() {
        when:
        def definition = new CouponDefinition(CODE, 100, COUNTRY_CODE)

        then:
        definition.code() == CODE
        definition.maxAllowedUsage() == 100
        definition.countryCode() == COUNTRY_CODE
    }

    def "should allow coupon to be used exactly once"() {
        when:
        def definition = new CouponDefinition(CODE, 1, COUNTRY_CODE)

        then:
        definition.maxAllowedUsage() == 1
    }

    def "should reject max allowed usage #maxAllowedUsage"() {
        when:
        new CouponDefinition(CODE, maxAllowedUsage, COUNTRY_CODE)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "It should be possible to use a coupon at least once"

        where:
        maxAllowedUsage << [0, -1, Integer.MIN_VALUE]
    }

    def "should reject null #field"() {
        when:
        new CouponDefinition(code, 100, countryCode)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "${field} must not be null"

        where:
        field         | code | countryCode
        "code"        | null | COUNTRY_CODE
        "countryCode" | CODE | null
    }
}
