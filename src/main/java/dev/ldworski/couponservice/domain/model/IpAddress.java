package dev.ldworski.couponservice.domain.model;

import java.net.InetAddress;
import java.util.Objects;

public class IpAddress {

    private final InetAddress ipAddress;

    public IpAddress(String value) {
        Objects.requireNonNull(value, "value must not be null");
        this.ipAddress = InetAddress.ofLiteral(value);
    }

    public String getValue() {
        return ipAddress.getHostAddress();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IpAddress that && ipAddress.equals(that.ipAddress);
    }

    @Override
    public int hashCode() {
        return ipAddress.hashCode();
    }

    @Override
    public String toString() {
        return getValue();
    }
}
