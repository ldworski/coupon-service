package dev.ldworski.couponservice.domain.model;

import java.util.Objects;

public record CouponDefinition(CouponCode code, int maxAllowedUsage, CountryCode countryCode) {

    public CouponDefinition {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(countryCode, "countryCode must not be null");
        if (maxAllowedUsage <= 0) {
            throw new IllegalArgumentException("It should be possible to use a couple at least once");
        }
    }
}
