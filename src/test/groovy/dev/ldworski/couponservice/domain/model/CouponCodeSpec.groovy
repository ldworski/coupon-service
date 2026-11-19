package dev.ldworski.couponservice.domain.model

import spock.lang.Specification

class CouponCodeSpec extends Specification {

    def "should normalize '#rawValue' to '#expectedValue'"() {
        when:
        def couponCode = new CouponCode(rawValue)

        then:
        couponCode.value() == expectedValue

        where:
        rawValue         || expectedValue
        "WINTER2026"     || "WINTER2026"
        "winter2026"     || "WINTER2026"
        "Winter2026"     || "WINTER2026"
        "  WINTER2026  " || "WINTER2026"
        "\twinter2026\n" || "WINTER2026"
    }

    def "should treat codes differing only by case and surrounding whitespace as equal"() {
        expect:
        new CouponCode(" winter2026 ") == new CouponCode("WINTER2026")
    }

    def "should reject null value"() {
        when:
        new CouponCode(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "value must not be null"
    }

    def "should reject #description value"() {
        when:
        new CouponCode(value)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "value must not be blank"

        where:
        description       | value
        "empty"           | ""
        "whitespace-only" | "   "
        "tabs-and-lines"  | "\t\n"
    }
}
