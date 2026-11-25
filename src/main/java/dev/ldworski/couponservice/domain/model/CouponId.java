package dev.ldworski.couponservice.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CouponId(UUID value) {

    public CouponId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static CouponId newCouponId() {
        return new CouponId(UUID.randomUUID());
    }
}
