package dev.ldworski.couponservice.domain.service;

import static dev.ldworski.couponservice.domain.model.Coupon.newCoupon;

import dev.ldworski.couponservice.domain.port.in.RegisterCouponUseCase;
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort;
import java.time.Clock;
import java.util.UUID;

public class RegisterCouponService implements RegisterCouponUseCase {

    private final CouponRepositoryPort couponRepositoryPort;
    private final Clock clock;

    public RegisterCouponService(CouponRepositoryPort couponRepositoryPort, Clock clock) {
        this.couponRepositoryPort = couponRepositoryPort;
        this.clock = clock;
    }

    @Override
    public UUID registerCoupon(RegisterCouponCodeCommand command) {
        var newCoupon = newCoupon(command.couponDefinition(), clock.instant());
        return couponRepositoryPort.registerCoupon(newCoupon).getId();
    }
}
