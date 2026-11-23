package dev.ldworski.couponservice.domain.model;

import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInLocationException;
import dev.ldworski.couponservice.domain.exception.CouponUsageLimitReachedException;
import java.time.Instant;
import java.util.Objects;

public class Coupon {
    private final CouponId id;
    private final CouponDefinition couponDefinition;
    private final int currentUsages;
    private final Instant creationDate;

    public Coupon(CouponId id, CouponDefinition couponDefinition, int currentUsages, Instant creationDate) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.couponDefinition = Objects.requireNonNull(couponDefinition, "couponDefinition must not be null");
        if (currentUsages < 0) {
            throw new IllegalArgumentException("current usages cannot be negative");
        }
        if (currentUsages > couponDefinition.maxAllowedUsage()) {
            throw new IllegalArgumentException("current usages cannot exceed max allowed usage");
        }
        this.currentUsages = currentUsages;
        this.creationDate = Objects.requireNonNull(creationDate, "creationDate must not be null");
    }

    public static Coupon newCoupon(CouponDefinition couponDefinition, Instant creationDate) {
        return new Coupon(CouponId.newCouponId(), couponDefinition, 0, creationDate);
    }

    public Coupon tryToRedeem() {
        validateUsageLimitNotReached();
        return new Coupon(id, couponDefinition, currentUsages + 1, creationDate);
    }

    public void validateUsageLimitNotReached() {
        if (isUsageLimitReached()) {
            throw new CouponUsageLimitReachedException(couponDefinition.code());
        }
    }

    public void validateAvailableIn(CountryCode countryCode) {
        if (couponDefinition.countryCode() != countryCode) {
            throw new CouponNotAvailableInLocationException(couponDefinition.code());
        }
    }

    public boolean isUsageLimitReached() {
        return currentUsages >= couponDefinition.maxAllowedUsage();
    }

    public CouponId getId() {
        return id;
    }

    public CouponDefinition getCouponDefinition() {
        return couponDefinition;
    }

    public int getCurrentUsages() {
        return currentUsages;
    }

    public Instant getCreationDate() {
        return creationDate;
    }
}
