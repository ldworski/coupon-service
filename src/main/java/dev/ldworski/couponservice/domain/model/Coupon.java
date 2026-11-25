package dev.ldworski.couponservice.domain.model;

import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInLocationException;
import dev.ldworski.couponservice.domain.exception.CouponUsageLimitReachedException;
import java.time.Instant;
import java.util.Objects;

public record Coupon(CouponId id, CouponCode code, CountryCode countryCode, CouponUsage usage, Instant creationDate) {

    public Coupon {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(countryCode, "countryCode must not be null");
        Objects.requireNonNull(usage, "usage must not be null");
        Objects.requireNonNull(creationDate, "creationDate must not be null");
    }

    public static Coupon newCoupon(
            CouponCode code, CountryCode countryCode, int maxAllowedUsage, Instant creationDate) {
        return new Coupon(CouponId.newCouponId(), code, countryCode, CouponUsage.unused(maxAllowedUsage), creationDate);
    }

    public Coupon tryToRedeem() {
        validateUsageLimitNotReached();
        return new Coupon(id, code, countryCode, usage.incremented(), creationDate);
    }

    public void validateUsageLimitNotReached() {
        if (isUsageLimitReached()) {
            throw new CouponUsageLimitReachedException(code);
        }
    }

    public void validateAvailableIn(CountryCode countryCode) {
        if (this.countryCode != countryCode) {
            throw new CouponNotAvailableInLocationException(code);
        }
    }

    public boolean isUsageLimitReached() {
        return usage.isLimitReached();
    }
}
