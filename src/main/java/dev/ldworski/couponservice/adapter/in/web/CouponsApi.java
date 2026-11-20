package dev.ldworski.couponservice.adapter.in.web;

import dev.ldworski.couponservice.adapter.in.web.dto.RegisterCouponRequest;
import dev.ldworski.couponservice.adapter.in.web.dto.RegisterCouponResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "Coupons", description = "Coupon management")
interface CouponsApi {

    @Operation(summary = "Register a new coupon", description = """
        Registers a coupon with a unique code. \
        The code may contain only uppercase letters A-Z and digits.\
        """)
    @ApiResponse(
            responseCode = "201",
            description = "Coupon registered",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegisterCouponResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "Request validation failed or request body is malformed",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Coupon with the given code already exists",
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
    ResponseEntity<RegisterCouponResponse> registerCoupon(RegisterCouponRequest registerCouponRequest);
}
