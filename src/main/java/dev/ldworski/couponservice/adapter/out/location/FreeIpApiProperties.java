package dev.ldworski.couponservice.adapter.out.location;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("coupon-service.free-ip-api")
public record FreeIpApiProperties(String baseUrl, Duration connectTimeout, Duration readTimeout, Retry retry) {

    public record Retry(long maxRetries, Duration delay, double multiplier, Duration jitter, Duration maxDelay) {}
}
