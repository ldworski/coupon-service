package dev.ldworski.couponservice.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("coupon-service.cache")
public record CacheProperties(
        @NotNull @Valid CacheSettings ipCountries, @NotNull @Valid CacheSettings exhaustedCoupons) {

    public record CacheSettings(@NotNull Long maximumSize, @NotNull Duration expireAfterWrite) {}
}
