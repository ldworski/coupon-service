package dev.ldworski.couponservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Coupon {
    private final UUID id;
    private final CouponDefinition couponDefinition;
    private final int currentUsages;
    private final Instant creationDate;

    public Coupon(UUID id, CouponDefinition couponDefinition, int currentUsages, Instant creationDate) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.couponDefinition = Objects.requireNonNull(couponDefinition, "couponDefinition must not be null");
        if (currentUsages < 0) {
            throw new IllegalArgumentException("current usages cannot be negative");
        }
        this.currentUsages = currentUsages;
        this.creationDate = Objects.requireNonNull(creationDate, "creationDate must not be null");
    }

    public static Coupon newCoupon(CouponDefinition couponDefinition, Instant creationDate) {
        return new Coupon(UUID.randomUUID(), couponDefinition, 0, creationDate);
    }

    public UUID getId() {
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
