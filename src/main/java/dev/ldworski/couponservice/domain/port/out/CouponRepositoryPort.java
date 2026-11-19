package dev.ldworski.couponservice.domain.port.out;

import dev.ldworski.couponservice.domain.model.Coupon;

public interface CouponRepositoryPort {

    Coupon registerCoupon(Coupon coupon);
}
