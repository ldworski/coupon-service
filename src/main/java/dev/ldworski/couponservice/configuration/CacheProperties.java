package dev.ldworski.couponservice.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("coupon-service.cache")
public record CacheProperties(CacheSettings ipCountries, CacheSettings exhaustedCoupons) {

    public record CacheSettings(long maximumSize, Duration expireAfterWrite) {
    }
}
