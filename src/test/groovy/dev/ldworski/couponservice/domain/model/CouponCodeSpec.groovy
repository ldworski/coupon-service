package dev.ldworski.couponservice.domain.model

import spock.lang.Specification

class CouponCodeSpec extends Specification {

    def "should accept code '#value'"() {
        when:
        def couponCode = new CouponCode(value)

        then:
        couponCode.value() == value

        where:
        value << ["WINTER2026", "2026", "A", "7", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", 'A' * CouponCode.MAX_LENGTH]
    }

    def "should reject null value"() {
        when:
        new CouponCode(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "value must not be null"
    }

    def "should reject empty value"() {
        when:
        new CouponCode("")

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "value must not be blank"
    }

    def "should reject value longer than maximum length"() {
        when:
        new CouponCode('A' * (CouponCode.MAX_LENGTH + 1))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "value must not be longer than 255 characters"
    }

    def "should reject value when #description"() {
        when:
        new CouponCode(value)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "value must contain only uppercase letters A-Z or digits"

        where:
        description                       | value
        "it is lower case"                | "winter2026"
        "it is mixed case"                | "Winter2026"
        "it is whitespace only"           | "   "
        "it has leading whitespace"       | " WINTER2026"
        "it has trailing whitespace"      | "WINTER2026 "
        "it contains inner whitespace"    | "WINTER 2026"
        "it contains tab"                 | "WINTER\t2026"
        "it contains new line"            | "WINTER\n2026"
        "it contains non-breaking space"  | "WINTER 2026"
        "it contains dash"                | "WINTER-2026"
        "it contains underscore"          | "WINTER_2026"
        "it contains hash"                | "WINTER#2026"
        "it contains Polish letter"       | "ZIMAŚ2026"
        "it contains Cyrillic look-alike" | "ZIMА2026"
        "it contains non-ASCII digit"     | "ZIMA٢٠٢٦"
    }
}
