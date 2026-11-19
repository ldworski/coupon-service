package dev.ldworski.couponservice.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Result of coupon registration")
public record RegisterCouponResponse(
        @NotNull
        @Schema(
                description = "Identifier of the registered coupon",
                example = "0f8fad5b-d9cb-469f-a165-70867728950e",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID couponId) {}
