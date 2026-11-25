package dev.ldworski.couponservice.domain.model;

import java.time.Instant;
import java.util.Objects;

public record CouponRedemption(CouponId couponId, UserId userId, Instant redemptionDate) {

    public CouponRedemption {
        Objects.requireNonNull(couponId, "couponId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(redemptionDate, "redemptionDate must not be null");
    }
}
