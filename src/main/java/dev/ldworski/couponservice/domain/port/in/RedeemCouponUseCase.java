package dev.ldworski.couponservice.domain.port.in;

import dev.ldworski.couponservice.domain.model.CouponCode;
import dev.ldworski.couponservice.domain.model.IpAddress;
import dev.ldworski.couponservice.domain.model.UserId;

public interface RedeemCouponUseCase {

    void redeemCoupon(RedeemCouponCommand command);

    record RedeemCouponCommand(CouponCode code, UserId userId, IpAddress ipAddress) {}
}
