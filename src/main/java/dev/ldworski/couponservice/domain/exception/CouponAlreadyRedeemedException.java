package dev.ldworski.couponservice.domain.exception;

import dev.ldworski.couponservice.domain.model.CouponId;
import dev.ldworski.couponservice.domain.model.UserId;

public class CouponAlreadyRedeemedException extends RuntimeException {

    private final CouponId couponId;
    private final UserId userId;

    public CouponAlreadyRedeemedException(CouponId couponId, UserId userId) {
        this(couponId, userId, null);
    }

    public CouponAlreadyRedeemedException(CouponId couponId, UserId userId, Throwable cause) {
        super("Coupon %s has already been redeemed by user %s".formatted(couponId.value(), userId.value()), cause);
        this.couponId = couponId;
        this.userId = userId;
    }

    public CouponId getCouponId() {
        return couponId;
    }

    public UserId getUserId() {
        return userId;
    }
}
