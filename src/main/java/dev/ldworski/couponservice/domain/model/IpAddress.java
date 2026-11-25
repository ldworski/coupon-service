package dev.ldworski.couponservice.domain.model;

import java.util.Objects;
import org.apache.commons.validator.routines.InetAddressValidator;

public record IpAddress(String value) {

    public IpAddress {
        Objects.requireNonNull(value, "value must not be null");
        if (!isValid(value)) {
            throw new IllegalArgumentException("value must be a valid IPv4 or IPv6 address");
        }
    }

    public static boolean isValid(String value) {
        return InetAddressValidator.getInstance().isValid(value);
    }
}
