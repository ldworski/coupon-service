package dev.ldworski.couponservice.adapter.in.web.dto;

import dev.ldworski.couponservice.adapter.in.web.validation.ValidIpAddress;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request to redeem a coupon")
public record RedeemCouponRequest(
        @NotNull
        @Schema(description = "Identifier of the user redeeming the coupon", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID userId,

        @NotNull
        @ValidIpAddress
        @Schema(
                description = "IPv4 or IPv6 address of the user, used to determine the user's country",
                example = "83.0.0.1")
        String ipAddress) {}
