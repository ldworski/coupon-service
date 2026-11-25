package dev.ldworski.couponservice.domain.service;

import dev.ldworski.couponservice.domain.exception.CouponAlreadyRedeemedException;
import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInLocationException;
import dev.ldworski.couponservice.domain.exception.CouponNotFoundException;
import dev.ldworski.couponservice.domain.model.Coupon;
import dev.ldworski.couponservice.domain.model.CouponCode;
import dev.ldworski.couponservice.domain.model.CouponId;
import dev.ldworski.couponservice.domain.model.CouponRedemption;
import dev.ldworski.couponservice.domain.model.IpAddress;
import dev.ldworski.couponservice.domain.model.UserId;
import dev.ldworski.couponservice.domain.port.in.RedeemCouponUseCase;
import dev.ldworski.couponservice.domain.port.out.CountryIdentificationPort;
import dev.ldworski.couponservice.domain.port.out.CouponRedemptionRepositoryPort;
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort;
import dev.ldworski.couponservice.domain.port.out.TransactionPort;
import java.time.Clock;

public class RedeemCouponService implements RedeemCouponUseCase {

    private final CountryIdentificationPort countryIdentificationPort;
    private final CouponRepositoryPort couponRepositoryPort;
    private final CouponRedemptionRepositoryPort couponRedemptionRepositoryPort;
    private final TransactionPort transactionPort;
    private final Clock clock;

    public RedeemCouponService(
            CountryIdentificationPort countryIdentificationPort,
            CouponRepositoryPort couponRepositoryPort,
            CouponRedemptionRepositoryPort couponRedemptionRepositoryPort,
            TransactionPort transactionPort,
            Clock clock) {
        this.countryIdentificationPort = countryIdentificationPort;
        this.couponRepositoryPort = couponRepositoryPort;
        this.couponRedemptionRepositoryPort = couponRedemptionRepositoryPort;
        this.transactionPort = transactionPort;
        this.clock = clock;
    }

    @Override
    public void redeemCoupon(RedeemCouponCommand command) {
        var coupon = couponRepositoryPort
                .findByCode(command.code())
                .orElseThrow(() -> new CouponNotFoundException(command.code()));
        verifyRedemptionPossibility(coupon, command.userId(), command.ipAddress());
        tryToRedeemCoupon(coupon.id(), coupon.code(), command.userId());
    }

    private void verifyRedemptionPossibility(Coupon coupon, UserId userId, IpAddress ipAddress) {
        if (couponRedemptionRepositoryPort.isRedeemed(coupon.id(), userId)) {
            throw new CouponAlreadyRedeemedException(coupon.id(), userId);
        }
        coupon.validateUsageLimitNotReached();
        var countryCode = countryIdentificationPort
                .identifyCountry(ipAddress)
                .orElseThrow(() -> new CouponNotAvailableInLocationException(coupon.code()));
        coupon.validateAvailableIn(countryCode);
    }

    private void tryToRedeemCoupon(CouponId couponId, CouponCode couponCode, UserId userId) {
        transactionPort.inTransaction(() -> {
            var coupon = couponRepositoryPort
                    .findByCodeForUpdate(couponCode)
                    .orElseThrow(() -> new CouponNotFoundException(couponCode));
            couponRepositoryPort.updateUsage(coupon.tryToRedeem());
            couponRedemptionRepositoryPort.registerRedemption(new CouponRedemption(couponId, userId, clock.instant()));
        });
    }
}
