package dev.ldworski.couponservice.domain.service

import dev.ldworski.couponservice.domain.model.CountryCode
import dev.ldworski.couponservice.domain.model.Coupon
import dev.ldworski.couponservice.domain.model.CouponCode
import dev.ldworski.couponservice.domain.model.CouponId
import dev.ldworski.couponservice.domain.model.CouponUsage
import dev.ldworski.couponservice.domain.port.in.RegisterCouponUseCase.RegisterCouponCodeCommand
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import spock.lang.Specification
import spock.lang.Subject

class RegisterCouponServiceSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-19T18:15:30Z")
    private static final String CODE = "WINTER2026"
    private static final int MAX_ALLOWED_USAGE = 100
    private static final CountryCode COUNTRY_CODE = CountryCode.PL
    private static final RegisterCouponCodeCommand COMMAND =
            new RegisterCouponCodeCommand(new CouponCode(CODE), COUNTRY_CODE, MAX_ALLOWED_USAGE)

    CouponRepositoryPort couponRepositoryPort = Mock()

    @Subject
    RegisterCouponService service = new RegisterCouponService(couponRepositoryPort, Clock.fixed(NOW, ZoneOffset.UTC))

    def "should register new coupon with given values and current time"() {
        when:
        CouponId couponId = service.registerCoupon(COMMAND)

        then:
        couponId != null
        1 * couponRepositoryPort.registerCoupon(_) >> { Coupon coupon ->
            assert coupon != null
            assert coupon.code == new CouponCode(CODE)
            assert coupon.countryCode == COUNTRY_CODE
            assert coupon.usage == new CouponUsage(0, MAX_ALLOWED_USAGE)
            assert coupon.creationDate == NOW
            return coupon
        }
    }

    def "should return id of registered coupon"() {
        given:
        def registeredCoupon = Coupon.newCoupon(new CouponCode(CODE), COUNTRY_CODE, MAX_ALLOWED_USAGE, NOW)
        couponRepositoryPort.registerCoupon(_ as Coupon) >> registeredCoupon

        when:
        def id = service.registerCoupon(COMMAND)

        then:
        id == registeredCoupon.id
    }

    def "should propagate repository failure"() {
        given:
        def failure = new IllegalStateException("database unavailable")
        couponRepositoryPort.registerCoupon(_ as Coupon) >> { throw failure }

        when:
        service.registerCoupon(COMMAND)

        then:
        def thrownException = thrown(IllegalStateException)
        thrownException.is(failure)
    }
}
