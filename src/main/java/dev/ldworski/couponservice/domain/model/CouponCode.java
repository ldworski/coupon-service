package dev.ldworski.couponservice.domain.model;

import java.util.Objects;

public record CouponCode(String value) {

    public static final int MAX_LENGTH = 255;
    public static final String ALLOWED_CHARACTERS_REGEX = "[A-Z0-9]*";
    public static final String ALLOWED_CHARACTERS_MESSAGE = "must contain only uppercase letters A-Z or digits";

    public CouponCode {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("value must not be longer than " + MAX_LENGTH + " characters");
        }
        if (!value.matches(ALLOWED_CHARACTERS_REGEX)) {
            throw new IllegalArgumentException("value " + ALLOWED_CHARACTERS_MESSAGE);
        }
    }
}
