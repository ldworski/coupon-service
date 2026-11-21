package dev.ldworski.couponservice.domain.exception;

import dev.ldworski.couponservice.domain.model.CouponCode;

public class CouponUsageLimitReachedException extends RuntimeException {

    private final CouponCode code;

    public CouponUsageLimitReachedException(CouponCode code) {
        super("Coupon with code %s has reached its usage limit".formatted(code.value()));
        this.code = code;
    }

    public CouponCode getCode() {
        return code;
    }
}
