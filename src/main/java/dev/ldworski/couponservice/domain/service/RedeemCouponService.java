package dev.ldworski.couponservice.domain.service;

import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInLocationException;
import dev.ldworski.couponservice.domain.exception.CouponNotFoundException;
import dev.ldworski.couponservice.domain.model.CouponRedemption;
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
        var countryCode = countryIdentificationPort
                .identifyCountry(command.ipAddress())
                .orElseThrow(() -> new CouponNotAvailableInLocationException(command.code()));
        var coupon = couponRepositoryPort
                .findByCode(command.code())
                .orElseThrow(() -> new CouponNotFoundException(command.code()));
        coupon.validateForRedemption(countryCode);
        tryToRedeemCoupon(command);
    }

    private void tryToRedeemCoupon(RedeemCouponCommand command) {
        transactionPort.inTransaction(() -> {
            var lockedCoupon = couponRepositoryPort
                    .findByCodeForUpdate(command.code())
                    .orElseThrow(() -> new CouponNotFoundException(command.code()));
            couponRepositoryPort.updateUsage(lockedCoupon.tryToRedeem());
            couponRedemptionRepositoryPort.registerRedemption(
                    new CouponRedemption(lockedCoupon.getId(), command.userId(), clock.instant()));
        });
    }
}
