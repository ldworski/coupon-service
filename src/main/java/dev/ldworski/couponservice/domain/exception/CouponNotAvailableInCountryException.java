package dev.ldworski.couponservice.domain.exception;

import dev.ldworski.couponservice.domain.model.CouponCode;

public class CouponNotAvailableInCountryException extends RuntimeException {

    private final CouponCode code;

    public CouponNotAvailableInCountryException(CouponCode code) {
        super("Coupon with code %s is not available in user's country".formatted(code.value()));
        this.code = code;
    }

    public CouponCode getCode() {
        return code;
    }
}
