package dev.ldworski.couponservice.domain.model

import spock.lang.Specification

class CouponIdSpec extends Specification {

    def "should create coupon id from UUID"() {
        given:
        def uuid = UUID.randomUUID()

        expect:
        new CouponId(uuid).value() == uuid
    }

    def "should reject null value"() {
        when:
        new CouponId(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "value must not be null"
    }

    def "should generate different ids for new coupon ids"() {
        expect:
        CouponId.newCouponId() != CouponId.newCouponId()
    }
}
