package dev.ldworski.couponservice.domain.exception;

import dev.ldworski.couponservice.domain.model.CouponCode;

public class CouponNotFoundException extends RuntimeException {

    private final CouponCode code;

    public CouponNotFoundException(CouponCode code) {
        super("Coupon with code %s not found".formatted(code.value()));
        this.code = code;
    }

    public CouponCode getCode() {
        return code;
    }
}
