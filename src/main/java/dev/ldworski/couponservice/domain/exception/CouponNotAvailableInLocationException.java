package dev.ldworski.couponservice.domain.exception;

import dev.ldworski.couponservice.domain.model.CouponCode;

public class CouponNotAvailableInLocationException extends RuntimeException {

    public CouponNotAvailableInLocationException(CouponCode code) {
        super("Coupon with code %s is not available for user's location".formatted(code.value()));
    }
}
