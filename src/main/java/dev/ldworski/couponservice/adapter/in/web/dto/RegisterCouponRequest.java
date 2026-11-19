package dev.ldworski.couponservice.adapter.in.web.dto;

import dev.ldworski.couponservice.domain.model.CountryCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterCouponRequest(
        @NotNull @NotBlank String code,
        @Min(1) int maxUsagesAllowed,
        @NotNull CountryCode countryCode) {}
