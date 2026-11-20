package dev.ldworski.couponservice.domain.port.out;

import dev.ldworski.couponservice.domain.model.CountryCode;
import dev.ldworski.couponservice.domain.model.IpAddress;
import java.util.Optional;

public interface CountryIdentificationPort {

    Optional<CountryCode> identifyCountry(IpAddress ipAddress);
}
