package dev.ldworski.couponservice.adapter.in.web;

import dev.ldworski.couponservice.adapter.in.web.dto.RedeemCouponRequest;
import dev.ldworski.couponservice.domain.model.CouponCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "Coupon redemptions", description = "Coupon redemption")
interface CouponRedemptionsApi {

    @Operation(summary = "Redeem a coupon", description = """
        Redeems a coupon for the given user. \
        The coupon must be defined for the country determined from the user's IP address.\
        """)
    @ApiResponse(responseCode = "201", description = "Coupon redeemed")
    @ApiResponse(
            responseCode = "400",
            description = "Request validation failed or request body is malformed",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> redeemCoupon(
            @Parameter(description = "Code of the coupon to redeem", example = "WINTER2026")
                    @Size(max = CouponCode.MAX_LENGTH)
                    @Pattern(
                            regexp = CouponCode.ALLOWED_CHARACTERS_REGEX,
                            message = CouponCode.ALLOWED_CHARACTERS_MESSAGE)
                    String code,
            @Valid RedeemCouponRequest redeemCouponRequest);
}
