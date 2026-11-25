package dev.ldworski.couponservice.domain.exception;

import dev.ldworski.couponservice.domain.model.CouponCode;

public class CouponUsageLimitReachedException extends RuntimeException {

    public CouponUsageLimitReachedException(CouponCode code) {
        super("Coupon with code %s has reached its usage limit".formatted(code.value()));
    }
}
