package dev.ldworski.couponservice.domain.service

import dev.ldworski.couponservice.domain.exception.CouponAlreadyRedeemedException
import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInCountryException
import dev.ldworski.couponservice.domain.exception.CouponNotFoundException
import dev.ldworski.couponservice.domain.exception.CouponUsageLimitReachedException
import dev.ldworski.couponservice.domain.model.CountryCode
import dev.ldworski.couponservice.domain.model.Coupon
import dev.ldworski.couponservice.domain.model.CouponCode
import dev.ldworski.couponservice.domain.model.CouponDefinition
import dev.ldworski.couponservice.domain.model.CouponId
import dev.ldworski.couponservice.domain.model.CouponRedemption
import dev.ldworski.couponservice.domain.model.IpAddress
import dev.ldworski.couponservice.domain.model.UserId
import dev.ldworski.couponservice.domain.port.in.RedeemCouponUseCase.RedeemCouponCommand
import dev.ldworski.couponservice.domain.port.out.CountryIdentificationPort
import dev.ldworski.couponservice.domain.port.out.CouponRedemptionRepositoryPort
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort
import dev.ldworski.couponservice.domain.port.out.TransactionPort
import spock.lang.Specification
import spock.lang.Subject

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RedeemCouponServiceSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-21T18:15:30Z")
    private static final CouponCode CODE = new CouponCode("WINTER2026")
    private static final UserId USER_ID = new UserId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
    private static final IpAddress IP_ADDRESS = new IpAddress("83.0.0.1")
    private static final Coupon COUPON = new Coupon(
            CouponId.newCouponId(), new CouponDefinition(CODE, 10, CountryCode.PL), 0, Instant.parse("2026-11-01T00:00:00Z"))
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

    def "should register redemption and update coupon usage in one transaction"() {
        given:
        countryIdentificationPort.identifyCountry(IP_ADDRESS) >> Optional.of(CountryCode.PL)

        when:
        service.redeemCoupon(COMMAND)

        then:
        1 * transactionPort.inTransaction(_) >> { Runnable operation -> operation.run() }

        then:
        1 * couponRepositoryPort.findByCodeForUpdate(CODE) >> Optional.of(COUPON)

        then:
        1 * couponRedemptionRepositoryPort.registerRedemption(new CouponRedemption(COUPON.id, USER_ID, NOW))

        then:
        1 * couponRepositoryPort.updateUsage({ Coupon coupon -> coupon.id == COUPON.id && coupon.currentUsages == 1 })
    }

    def "should not redeem coupon when coupon does not exist"() {
        given:
        countryIdentificationPort.identifyCountry(IP_ADDRESS) >> Optional.of(CountryCode.PL)
        transactionPort.inTransaction(_) >> { Runnable operation -> operation.run() }
        couponRepositoryPort.findByCodeForUpdate(CODE) >> Optional.empty()

        when:
        service.redeemCoupon(COMMAND)

        then:
        def exception = thrown(CouponNotFoundException)
        exception.code == CODE
        0 * couponRedemptionRepositoryPort._
        0 * couponRepositoryPort.updateUsage(_)
    }

    def "should not redeem coupon when #description"() {
        given:
        countryIdentificationPort.identifyCountry(IP_ADDRESS) >> userCountry
        transactionPort.inTransaction(_) >> { Runnable operation -> operation.run() }
        couponRepositoryPort.findByCodeForUpdate(CODE) >> Optional.of(COUPON)

        when:
        service.redeemCoupon(COMMAND)

        then:
        def exception = thrown(CouponNotAvailableInCountryException)
        exception.code == CODE
        0 * couponRedemptionRepositoryPort._
        0 * couponRepositoryPort.updateUsage(_)

        where:
        description                                  | userCountry
        "coupon is defined for other country"        | Optional.of(CountryCode.DE)
        "user's country is not identified"           | Optional.empty()
    }

    def "should fail when coupon usage limit is reached"() {
        given:
        def exhaustedCoupon = new Coupon(COUPON.id, COUPON.couponDefinition, 10, COUPON.creationDate)
        countryIdentificationPort.identifyCountry(IP_ADDRESS) >> Optional.of(CountryCode.PL)
        transactionPort.inTransaction(_) >> { Runnable operation -> operation.run() }
        couponRepositoryPort.findByCodeForUpdate(CODE) >> Optional.of(exhaustedCoupon)

        when:
        service.redeemCoupon(COMMAND)

        then:
        def exception = thrown(CouponUsageLimitReachedException)
        exception.code == CODE
        0 * couponRepositoryPort.updateUsage(_)
    }

    def "should not update coupon usage when user has already redeemed coupon"() {
        given:
        countryIdentificationPort.identifyCountry(IP_ADDRESS) >> Optional.of(CountryCode.PL)
        transactionPort.inTransaction(_) >> { Runnable operation -> operation.run() }
        couponRepositoryPort.findByCodeForUpdate(CODE) >> Optional.of(COUPON)
        def alreadyRedeemed = new CouponAlreadyRedeemedException(COUPON.id, USER_ID, null)
        couponRedemptionRepositoryPort.registerRedemption(_) >> { throw alreadyRedeemed }

        when:
        service.redeemCoupon(COMMAND)

        then:
        def exception = thrown(CouponAlreadyRedeemedException)
        exception.is(alreadyRedeemed)
        0 * couponRepositoryPort.updateUsage(_)
    }
}
