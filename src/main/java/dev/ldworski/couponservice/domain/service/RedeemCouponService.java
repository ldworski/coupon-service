package dev.ldworski.couponservice.domain.service;

import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInCountryException;
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
        var countryCode = countryIdentificationPort.identifyCountry(command.ipAddress());
        transactionPort.inTransaction(() -> {
            var coupon = couponRepositoryPort
                    .findByCodeForUpdate(command.code())
                    .orElseThrow(() -> new CouponNotFoundException(command.code()));
            if (countryCode.isEmpty() || !coupon.isAvailableIn(countryCode.get())) {
                throw new CouponNotAvailableInCountryException(command.code());
            }
            couponRedemptionRepositoryPort.registerRedemption(
                    new CouponRedemption(coupon.getId(), command.userId(), clock.instant()));
            couponRepositoryPort.updateUsage(coupon.redeem());
        });
    }
}
