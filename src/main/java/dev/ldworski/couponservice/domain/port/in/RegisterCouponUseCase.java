package dev.ldworski.couponservice.domain.port.in;

import dev.ldworski.couponservice.domain.model.CouponDefinition;
import dev.ldworski.couponservice.domain.model.CouponId;

public interface RegisterCouponUseCase {

    CouponId registerCoupon(RegisterCouponCodeCommand command);

    record RegisterCouponCodeCommand(CouponDefinition couponDefinition) {}
}
