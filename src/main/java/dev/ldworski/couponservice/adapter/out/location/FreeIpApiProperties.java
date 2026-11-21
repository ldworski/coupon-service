package dev.ldworski.couponservice.adapter.out.location;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("coupon-service.free-ip-api")
public record FreeIpApiProperties(
        @NotBlank String baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull @Valid Retry retry) {

    public record Retry(
            @NotNull Long maxRetries,
            @NotNull Duration delay,
            @NotNull Double multiplier,
            @NotNull Duration jitter,
            @NotNull Duration maxDelay) {}
}
