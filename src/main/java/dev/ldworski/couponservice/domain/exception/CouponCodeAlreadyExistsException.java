package dev.ldworski.couponservice.domain.exception;

import dev.ldworski.couponservice.domain.model.CouponCode;

public class CouponCodeAlreadyExistsException extends RuntimeException {

    public CouponCodeAlreadyExistsException(CouponCode code, Throwable cause) {
        super("Coupon with code %s already exists".formatted(code.value()), cause);
    }
}
