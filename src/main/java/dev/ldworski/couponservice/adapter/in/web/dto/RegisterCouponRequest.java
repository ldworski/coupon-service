package dev.ldworski.couponservice.adapter.in.web.dto;

import dev.ldworski.couponservice.domain.model.CountryCode;
import dev.ldworski.couponservice.domain.model.CouponCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new coupon")
public record RegisterCouponRequest(
        @NotNull
        @NotBlank
        @Size(max = CouponCode.MAX_LENGTH)
        @Pattern(regexp = CouponCode.ALLOWED_CHARACTERS_REGEX, message = CouponCode.ALLOWED_CHARACTERS_MESSAGE)
        @Schema(
                description = "Unique coupon code consisting of uppercase letters A-Z and digits only",
                example = "WINTER2026")
        String code,

        @Min(1)
        @Schema(
                description = "Maximum number of times the coupon can be used",
                example = "100",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int maxUsagesAllowed,

        @NotNull @Schema(description = "Country in which the coupon can be used", example = "PL")
        CountryCode countryCode) {}
