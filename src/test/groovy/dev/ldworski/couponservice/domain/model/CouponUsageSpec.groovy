package dev.ldworski.couponservice.domain.model

import spock.lang.Specification

class CouponUsageSpec extends Specification {

    def "should create usage with #current of #maxAllowed usages"() {
        when:
        def usage = new CouponUsage(current, maxAllowed)

        then:
        usage.current() == current
        usage.maxAllowed() == maxAllowed

        where:
        current | maxAllowed
        0       | 1
        5       | 100
        100     | 100
    }

    def "should reject max allowed usage #maxAllowed"() {
        when:
        new CouponUsage(0, maxAllowed)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "It should be possible to use a coupon at least once"

        where:
        maxAllowed << [0, -1, Integer.MIN_VALUE]
    }

    def "should reject negative current usages"() {
        when:
        new CouponUsage(-1, 100)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "current usages cannot be negative"
    }

    def "should reject current usages exceeding max allowed usage"() {
        when:
        new CouponUsage(101, 100)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "current usages cannot exceed max allowed usage"
    }

    def "should create unused usage"() {
        expect:
        CouponUsage.unused(100) == new CouponUsage(0, 100)
    }

    def "should return incremented usage without changing original"() {
        given:
        def usage = new CouponUsage(99, 100)

        expect:
        usage.incremented() == new CouponUsage(100, 100)
        usage == new CouponUsage(99, 100)
    }

    def "should reject incrementing when limit is reached"() {
        when:
        new CouponUsage(100, 100).incremented()

        then:
        thrown(IllegalArgumentException)
    }

    def "should report limit reached #expected for #current of #maxAllowed usages"() {
        expect:
        new CouponUsage(current, maxAllowed).isLimitReached() == expected

        where:
        current | maxAllowed || expected
        0       | 100        || false
        99      | 100        || false
        100     | 100        || true
    }
}
