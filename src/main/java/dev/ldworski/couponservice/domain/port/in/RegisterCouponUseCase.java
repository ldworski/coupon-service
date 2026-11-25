package dev.ldworski.couponservice.domain.port.in;

import dev.ldworski.couponservice.domain.model.CountryCode;
import dev.ldworski.couponservice.domain.model.CouponCode;
import dev.ldworski.couponservice.domain.model.CouponId;

public interface RegisterCouponUseCase {

    CouponId registerCoupon(RegisterCouponCodeCommand command);

    record RegisterCouponCodeCommand(CouponCode code, CountryCode countryCode, int maxAllowedUsage) {}
}
