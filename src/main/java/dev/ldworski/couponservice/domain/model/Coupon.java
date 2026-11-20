package dev.ldworski.couponservice.domain.model;

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
        this.currentUsages = currentUsages;
        this.creationDate = Objects.requireNonNull(creationDate, "creationDate must not be null");
    }

    public static Coupon newCoupon(CouponDefinition couponDefinition, Instant creationDate) {
        return new Coupon(CouponId.newCouponId(), couponDefinition, 0, creationDate);
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
