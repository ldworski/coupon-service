package dev.ldworski.couponservice.domain.port.in;

import dev.ldworski.couponservice.domain.model.CouponDefinition;
import java.util.UUID;

public interface RegisterCouponUseCase {

    UUID registerCoupon(RegisterCouponCodeCommand command);

    record RegisterCouponCodeCommand(CouponDefinition couponDefinition) {}
}
