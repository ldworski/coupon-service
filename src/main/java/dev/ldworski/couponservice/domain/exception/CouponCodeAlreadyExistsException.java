package dev.ldworski.couponservice.domain.exception;

import dev.ldworski.couponservice.domain.model.CouponCode;

public class CouponCodeAlreadyExistsException extends RuntimeException {

    private final CouponCode code;

    public CouponCodeAlreadyExistsException(CouponCode code, Throwable cause) {
        super("Coupon with code %s already exists".formatted(code.value()), cause);
        this.code = code;
    }

    public CouponCode getCode() {
        return code;
    }
}
