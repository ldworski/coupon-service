package dev.ldworski.couponservice.domain.service

import dev.ldworski.couponservice.domain.exception.CouponAlreadyRedeemedException
import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInLocationException
import dev.ldworski.couponservice.domain.exception.CouponNotFoundException
import dev.ldworski.couponservice.domain.exception.CouponUsageLimitReachedException
import dev.ldworski.couponservice.domain.model.CountryCode
import dev.ldworski.couponservice.domain.model.Coupon
import dev.ldworski.couponservice.domain.model.CouponCode
import dev.ldworski.couponservice.domain.model.CouponId
import dev.ldworski.couponservice.domain.model.CouponRedemption
import dev.ldworski.couponservice.domain.model.CouponUsage
import dev.ldworski.couponservice.domain.model.IpAddress
import dev.ldworski.couponservice.domain.model.UserId
import dev.ldworski.couponservice.domain.port.in.RedeemCouponUseCase.RedeemCouponCommand
import dev.ldworski.couponservice.domain.port.out.CountryIdentificationPort
import dev.ldworski.couponservice.domain.port.out.CouponRedemptionRepositoryPort
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort
import dev.ldworski.couponservice.domain.port.out.TransactionPort
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import spock.lang.Specification
import spock.lang.Subject

class RedeemCouponServiceSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-21T18:15:30Z")
    private static final CouponCode CODE = new CouponCode("WINTER2026")
    private static final UserId USER_ID = new UserId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
    private static final IpAddress IP_ADDRESS = new IpAddress("83.0.0.1")
    private static final Coupon COUPON = new Coupon(
            CouponId.newCouponId(), CODE, CountryCode.PL, new CouponUsage(0, 10), Instant.parse("2026-11-01T00:00:00Z"))
    private static final Coupon EXHAUSTED_COUPON =
            new Coupon(COUPON.id(), CODE, CountryCode.PL, new CouponUsage(10, 10), COUPON.creationDate())
    private static final RedeemCouponCommand COMMAND = new RedeemCouponCommand(CODE, USER_ID, IP_ADDRESS)

    CountryIdentificationPort countryIdentificationPort = Mock()
    CouponRepositoryPort couponRepositoryPort = Mock()
    CouponRedemptionRepositoryPort couponRedemptionRepositoryPort = Mock()
    TransactionPort transactionPort = Mock()

    @Subject
    RedeemCouponService service = new RedeemCouponService(
            countryIdentificationPort,
            couponRepositoryPort,
            couponRedemptionRepositoryPort,
            transactionPort,
            Clock.fixed(NOW, ZoneOffset.UTC))

    def "should pre-validate coupon and then update usage and register redemption in one transaction"() {
        when:
        service.redeemCoupon(COMMAND)

        then:
        1 * couponRepositoryPort.findByCode(CODE) >> Optional.of(COUPON)

        then:
        1 * couponRedemptionRepositoryPort.isRedeemed(COUPON.id(), USER_ID) >> false

        then:
        1 * countryIdentificationPort.identifyCountry(IP_ADDRESS) >> Optional.of(CountryCode.PL)

        then:
        1 * transactionPort.inTransaction(_) >> { Runnable operation -> operation.run() }

        then:
        1 * couponRepositoryPort.findByCodeForUpdate(CODE) >> Optional.of(COUPON)

        then:
        1 * couponRepositoryPort.updateUsage({ Coupon coupon -> coupon.id() == COUPON.id() && coupon.usage().current() == 1 })

        then:
        1 * couponRedemptionRepositoryPort.registerRedemption(new CouponRedemption(COUPON.id(), USER_ID, NOW))
    }

    def "should not identify country when coupon does not exist"() {
        given:
        couponRepositoryPort.findByCode(CODE) >> Optional.empty()

        when:
        service.redeemCoupon(COMMAND)

        then:
        def exception = thrown(CouponNotFoundException)
        exception.message == "Coupon with code WINTER2026 not found"
        0 * countryIdentificationPort._
        0 * transactionPort._
        0 * couponRedemptionRepositoryPort._
    }

    def "should not redeem coupon when #description"() {
        given:
        countryIdentificationPort.identifyCountry(IP_ADDRESS) >> userCountry
        couponRepositoryPort.findByCode(CODE) >> Optional.of(COUPON)

        when:
        service.redeemCoupon(COMMAND)

        then:
        def exception = thrown(CouponNotAvailableInLocationException)
        exception.message == "Coupon with code WINTER2026 is not available for user's location"
        0 * transactionPort._
        0 * couponRedemptionRepositoryPort.registerRedemption(_)

        where:
        description                           | userCountry
        "coupon is defined for other country" | Optional.of(CountryCode.DE)
        "user's country is not identified"    | Optional.empty()
    }

    def "should neither identify country nor open transaction when coupon usage limit is already reached"() {
        given:
        couponRepositoryPort.findByCode(CODE) >> Optional.of(EXHAUSTED_COUPON)

        when:
        service.redeemCoupon(COMMAND)

        then:
        def exception = thrown(CouponUsageLimitReachedException)
        exception.message == "Coupon with code WINTER2026 has reached its usage limit"
        0 * countryIdentificationPort._
        0 * transactionPort._
        0 * couponRedemptionRepositoryPort.registerRedemption(_)
    }

    def "should neither identify country nor open transaction when user has already redeemed #description"() {
        given:
        couponRepositoryPort.findByCode(CODE) >> Optional.of(coupon)
        couponRedemptionRepositoryPort.isRedeemed(COUPON.id(), USER_ID) >> true

        when:
        service.redeemCoupon(COMMAND)

        then:
        def exception = thrown(CouponAlreadyRedeemedException)
        exception.couponId == COUPON.id()
        exception.userId == USER_ID
        0 * countryIdentificationPort._
        0 * transactionPort._
        0 * couponRedemptionRepositoryPort.registerRedemption(_)

        where:
        description                                | coupon
        "coupon"                                   | COUPON
        "coupon whose usage limit is now reached"  | EXHAUSTED_COUPON
    }

    def "should fail when coupon usage limit is reached between pre-validation and lock"() {
        given:
        countryIdentificationPort.identifyCountry(IP_ADDRESS) >> Optional.of(CountryCode.PL)
        couponRepositoryPort.findByCode(CODE) >> Optional.of(COUPON)
        transactionPort.inTransaction(_ as Runnable) >> { Runnable operation -> operation.run() }
        couponRepositoryPort.findByCodeForUpdate(CODE) >> Optional.of(EXHAUSTED_COUPON)

        when:
        service.redeemCoupon(COMMAND)

        then:
        def exception = thrown(CouponUsageLimitReachedException)
        exception.message == "Coupon with code WINTER2026 has reached its usage limit"
        0 * couponRepositoryPort.updateUsage(_)
        0 * couponRedemptionRepositoryPort.registerRedemption(_)
    }

    def "should propagate already redeemed failure from redemption registration when user redeems concurrently"() {
        given:
        countryIdentificationPort.identifyCountry(IP_ADDRESS) >> Optional.of(CountryCode.PL)
        couponRepositoryPort.findByCode(CODE) >> Optional.of(COUPON)
        transactionPort.inTransaction(_ as Runnable) >> { Runnable operation -> operation.run() }
        couponRepositoryPort.findByCodeForUpdate(CODE) >> Optional.of(COUPON)
        def alreadyRedeemed = new CouponAlreadyRedeemedException(COUPON.id(), USER_ID, null)
        couponRedemptionRepositoryPort.registerRedemption(_ as CouponRedemption) >> { throw alreadyRedeemed }

        when:
        service.redeemCoupon(COMMAND)

        then:
        def exception = thrown(CouponAlreadyRedeemedException)
        exception.is(alreadyRedeemed)
    }
}
