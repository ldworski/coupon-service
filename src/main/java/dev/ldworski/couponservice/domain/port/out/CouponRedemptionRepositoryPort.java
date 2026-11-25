package dev.ldworski.couponservice.domain.port.out;

import dev.ldworski.couponservice.domain.model.CouponId;
import dev.ldworski.couponservice.domain.model.CouponRedemption;
import dev.ldworski.couponservice.domain.model.UserId;

public interface CouponRedemptionRepositoryPort {

    boolean isRedeemed(CouponId couponId, UserId userId);

    void registerRedemption(CouponRedemption couponRedemption);
}
