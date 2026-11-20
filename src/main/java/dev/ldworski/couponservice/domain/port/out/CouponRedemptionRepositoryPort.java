package dev.ldworski.couponservice.domain.port.out;

import dev.ldworski.couponservice.domain.model.CouponRedemption;

public interface CouponRedemptionRepositoryPort {

    void registerRedemption(CouponRedemption couponRedemption);
}
