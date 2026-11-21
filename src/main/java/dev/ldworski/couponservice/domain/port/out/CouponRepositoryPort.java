package dev.ldworski.couponservice.domain.port.out;

import dev.ldworski.couponservice.domain.model.CountryCode;
import dev.ldworski.couponservice.domain.model.Coupon;
import dev.ldworski.couponservice.domain.model.CouponCode;
import java.util.Optional;

public interface CouponRepositoryPort {

    Coupon registerCoupon(Coupon coupon);

    Optional<Coupon> findByCodeAndCountryCode(CouponCode code, CountryCode countryCode);

    Optional<Coupon> findByCodeForUpdate(CouponCode code);

    void updateUsage(Coupon coupon);
}
