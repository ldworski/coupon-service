package dev.ldworski.couponservice.domain.exception;

import dev.ldworski.couponservice.domain.model.CouponCode;

public class CouponTemporarilyLockedException extends RuntimeException {

    public CouponTemporarilyLockedException(CouponCode code, Throwable cause) {
        super("Coupon with code %s is temporarily locked".formatted(code.value()), cause);
    }
}
