package dev.ldworski.couponservice.domain.model

import dev.ldworski.couponservice.domain.exception.CouponUsageLimitReachedException
import spock.lang.Specification

import java.time.Instant

class CouponSpec extends Specification {

    private static final CouponId ID = new CouponId(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"))
    private static final Instant NOW = Instant.parse("2026-11-19T18:15:30Z")
    private static final CouponDefinition COUPON_DEFINITION =
            new CouponDefinition(new CouponCode("WINTER2026"), 100, CountryCode.PL)

    def "should create coupon with given values"() {
        when:
        def coupon = new Coupon(ID, COUPON_DEFINITION, 5, NOW)

        then:
        coupon.id == ID
        coupon.couponDefinition == COUPON_DEFINITION
        coupon.currentUsages == 5
        coupon.creationDate == NOW
    }

    def "should allow zero current usages"() {
        when:
        def coupon = new Coupon(ID, COUPON_DEFINITION, 0, NOW)

        then:
        coupon.currentUsages == 0
    }

    def "should reject negative current usages"() {
        when:
        new Coupon(ID, COUPON_DEFINITION, -1, NOW)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "current usages cannot be negative"
    }

    def "should reject current usages exceeding max allowed usage"() {
        when:
        new Coupon(ID, COUPON_DEFINITION, 101, NOW)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "current usages cannot exceed max allowed usage"
    }

    def "should allow current usages equal to max allowed usage"() {
        when:
        def coupon = new Coupon(ID, COUPON_DEFINITION, 100, NOW)

        then:
        coupon.currentUsages == 100
    }

    def "should return coupon with incremented usages when redeemed"() {
        given:
        def coupon = new Coupon(ID, COUPON_DEFINITION, 99, NOW)

        when:
        def redeemed = coupon.redeem()

        then:
        redeemed.currentUsages == 100
        redeemed.id == ID
        redeemed.couponDefinition == COUPON_DEFINITION
        redeemed.creationDate == NOW
        coupon.currentUsages == 99
    }

    def "should not redeem coupon when usage limit is reached"() {
        given:
        def coupon = new Coupon(ID, COUPON_DEFINITION, 100, NOW)

        when:
        coupon.redeem()

        then:
        def exception = thrown(CouponUsageLimitReachedException)
        exception.code == COUPON_DEFINITION.code()
    }

    def "should be available only in country it is defined for: #countryCode"() {
        expect:
        new Coupon(ID, COUPON_DEFINITION, 0, NOW).isAvailableIn(countryCode) == available

        where:
        countryCode    || available
        CountryCode.PL || true
        CountryCode.DE || false
    }

    def "should reject null #field"() {
        when:
        new Coupon(id, couponDefinition, 0, creationDate)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "${field} must not be null"

        where:
        field              | id   | couponDefinition  | creationDate
        "id"               | null | COUPON_DEFINITION | NOW
        "couponDefinition" | ID   | null              | NOW
        "creationDate"     | ID   | COUPON_DEFINITION | null
    }

    def "should create new coupon with random id and zero usages"() {
        when:
        def coupon = Coupon.newCoupon(COUPON_DEFINITION, NOW)

        then:
        coupon.id != null
        coupon.couponDefinition == COUPON_DEFINITION
        coupon.currentUsages == 0
        coupon.creationDate == NOW
    }

    def "should generate different ids for new coupons"() {
        when:
        def first = Coupon.newCoupon(COUPON_DEFINITION, NOW)
        def second = Coupon.newCoupon(COUPON_DEFINITION, NOW)

        then:
        first.id != second.id
    }

    def "should reject null #field when creating new coupon"() {
        when:
        Coupon.newCoupon(couponDefinition, creationDate)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "${field} must not be null"

        where:
        field              | couponDefinition  | creationDate
        "couponDefinition" | null              | NOW
        "creationDate"     | COUPON_DEFINITION | null
    }
}
