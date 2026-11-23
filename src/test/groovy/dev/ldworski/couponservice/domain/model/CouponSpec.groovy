package dev.ldworski.couponservice.domain.model

import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInLocationException
import dev.ldworski.couponservice.domain.exception.CouponUsageLimitReachedException
import java.time.Instant
import spock.lang.Specification

class CouponSpec extends Specification {

    private static final CouponId ID = new CouponId(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"))
    private static final CouponCode CODE = new CouponCode("WINTER2026")
    private static final CountryCode COUNTRY_CODE = CountryCode.PL
    private static final Instant NOW = Instant.parse("2026-11-19T18:15:30Z")

    def "should create coupon with given values"() {
        when:
        def coupon = new Coupon(ID, CODE, COUNTRY_CODE, new CouponUsage(5, 100), NOW)

        then:
        coupon.id == ID
        coupon.code == CODE
        coupon.countryCode == COUNTRY_CODE
        coupon.usage == new CouponUsage(5, 100)
        coupon.creationDate == NOW
    }

    def "should return coupon with incremented usages when redeemed"() {
        given:
        def coupon = couponWithUsages(99)

        when:
        def redeemed = coupon.tryToRedeem()

        then:
        redeemed.usage == new CouponUsage(100, 100)
        redeemed.id == ID
        redeemed.code == CODE
        redeemed.countryCode == COUNTRY_CODE
        redeemed.creationDate == NOW
        coupon.usage == new CouponUsage(99, 100)
    }

    def "should not redeem coupon when usage limit is reached"() {
        when:
        couponWithUsages(100).tryToRedeem()

        then:
        def exception = thrown(CouponUsageLimitReachedException)
        exception.message == "Coupon with code WINTER2026 has reached its usage limit"
    }

    def "should pass usage limit validation when #currentUsages usages are below limit"() {
        when:
        couponWithUsages(currentUsages).validateUsageLimitNotReached()

        then:
        noExceptionThrown()

        where:
        currentUsages << [0, 99]
    }

    def "should fail usage limit validation when usage limit is reached"() {
        when:
        couponWithUsages(100).validateUsageLimitNotReached()

        then:
        def exception = thrown(CouponUsageLimitReachedException)
        exception.message == "Coupon with code WINTER2026 has reached its usage limit"
    }

    def "should report usage limit reached #expected for #currentUsages usages"() {
        expect:
        couponWithUsages(currentUsages).usageLimitReached == expected

        where:
        currentUsages || expected
        99            || false
        100           || true
    }

    def "should pass availability validation when coupon is defined for given country"() {
        when:
        couponWithUsages(0).validateAvailableIn(CountryCode.PL)

        then:
        noExceptionThrown()
    }

    def "should fail availability validation when coupon is defined for other country"() {
        when:
        couponWithUsages(0).validateAvailableIn(CountryCode.DE)

        then:
        def exception = thrown(CouponNotAvailableInLocationException)
        exception.message == "Coupon with code WINTER2026 is not available for user's location"
    }

    def "should reject null #field"() {
        when:
        new Coupon(id, code, countryCode, usage, creationDate)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "${field} must not be null"

        where:
        field          | id   | code | countryCode  | usage                 | creationDate
        "id"           | null | CODE | COUNTRY_CODE | new CouponUsage(0, 1) | NOW
        "code"         | ID   | null | COUNTRY_CODE | new CouponUsage(0, 1) | NOW
        "countryCode"  | ID   | CODE | null         | new CouponUsage(0, 1) | NOW
        "usage"        | ID   | CODE | COUNTRY_CODE | null                  | NOW
        "creationDate" | ID   | CODE | COUNTRY_CODE | new CouponUsage(0, 1) | null
    }

    def "should create new coupon with random id and zero usages"() {
        when:
        def coupon = Coupon.newCoupon(CODE, COUNTRY_CODE, 100, NOW)

        then:
        coupon.id != null
        coupon.code == CODE
        coupon.countryCode == COUNTRY_CODE
        coupon.usage == new CouponUsage(0, 100)
        coupon.creationDate == NOW
    }

    def "should generate different ids for new coupons"() {
        when:
        def first = Coupon.newCoupon(CODE, COUNTRY_CODE, 100, NOW)
        def second = Coupon.newCoupon(CODE, COUNTRY_CODE, 100, NOW)

        then:
        first.id != second.id
    }

    def "should reject invalid max allowed usage when creating new coupon"() {
        when:
        Coupon.newCoupon(CODE, COUNTRY_CODE, 0, NOW)

        then:
        thrown(IllegalArgumentException)
    }

    private static Coupon couponWithUsages(int currentUsages) {
        new Coupon(ID, CODE, COUNTRY_CODE, new CouponUsage(currentUsages, 100), NOW)
    }
}
