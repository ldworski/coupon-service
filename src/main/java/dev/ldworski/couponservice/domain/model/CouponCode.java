package dev.ldworski.couponservice.domain.model;

import java.util.Objects;

public record CouponCode(String value) {

    public CouponCode {
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim().toUpperCase();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
