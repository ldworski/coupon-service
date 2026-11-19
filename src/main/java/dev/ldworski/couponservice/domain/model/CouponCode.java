package dev.ldworski.couponservice.domain.model;

import java.util.Objects;

public record CouponCode(String value) {

    public CouponCode {
        Objects.requireNonNull(value, "Value must not be null");
        value = value.trim().toUpperCase();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Value must be not blank");
        }
    }
}
