package dev.ldworski.couponservice.domain.service

import dev.ldworski.couponservice.domain.model.CountryCode
import dev.ldworski.couponservice.domain.model.Coupon
import dev.ldworski.couponservice.domain.model.CouponCode
import dev.ldworski.couponservice.domain.model.CouponDefinition
import dev.ldworski.couponservice.domain.model.CouponId
import dev.ldworski.couponservice.domain.port.in.RegisterCouponUseCase.RegisterCouponCodeCommand
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort
import spock.lang.Specification
import spock.lang.Subject

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RegisterCouponServiceSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-19T18:15:30Z")
    private static final String CODE = "WINTER2026"
    private static final int MAX_ALLOWED_USAGE = 100
    private static final CountryCode COUNTRY_CODE = CountryCode.PL
    private static final CouponDefinition COUPON_DEFINITION =
            new CouponDefinition(new CouponCode(CODE), MAX_ALLOWED_USAGE, COUNTRY_CODE)

    CouponRepositoryPort couponRepositoryPort = Mock()

    @Subject
    RegisterCouponService service = new RegisterCouponService(couponRepositoryPort, Clock.fixed(NOW, ZoneOffset.UTC))

    def "should register new coupon with given definition and current time"() {
        when:
        CouponId couponId = service.registerCoupon(new RegisterCouponCodeCommand(COUPON_DEFINITION))

        then:
        couponId != null
        1 * couponRepositoryPort.registerCoupon(_) >> { Coupon coupon ->
            assert coupon != null
            assert coupon.couponDefinition == COUPON_DEFINITION
            assert coupon.currentUsages == 0
            assert coupon.creationDate == NOW
            return coupon
        }
    }

    def "should return id of registered coupon"() {
        given:
        def registeredCoupon = new Coupon(CouponId.newCouponId(), COUPON_DEFINITION, 0, NOW)
        couponRepositoryPort.registerCoupon(_ as Coupon) >> registeredCoupon

        when:
        def id = service.registerCoupon(new RegisterCouponCodeCommand(COUPON_DEFINITION))

        then:
        id == registeredCoupon.id
    }

    def "should propagate repository failure"() {
        given:
        def failure = new IllegalStateException("database unavailable")
        couponRepositoryPort.registerCoupon(_ as Coupon) >> { throw failure }

        when:
        service.registerCoupon(new RegisterCouponCodeCommand(COUPON_DEFINITION))

        then:
        def thrownException = thrown(IllegalStateException)
        thrownException.is(failure)
    }
}
