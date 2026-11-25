package dev.ldworski.couponservice.adapter.out.persistence;

import dev.ldworski.couponservice.domain.model.Coupon;
import dev.ldworski.couponservice.domain.model.CouponCode;
import dev.ldworski.couponservice.domain.model.CouponId;
import dev.ldworski.couponservice.domain.model.CouponUsage;
import org.springframework.stereotype.Component;

@Component
public class CouponPersistenceMapper {

    public Coupon from(CouponEntity couponEntity) {
        return new Coupon(
                new CouponId(couponEntity.getId()),
                new CouponCode(couponEntity.getCode()),
                couponEntity.getCountryCode(),
                new CouponUsage(couponEntity.getCurrentUsage(), couponEntity.getMaxAllowedUsage()),
                couponEntity.getCreated());
    }

    public CouponEntity from(Coupon coupon) {
        return new CouponEntity(
                coupon.id().value(),
                coupon.code().value(),
                coupon.usage().maxAllowed(),
                coupon.usage().current(),
                coupon.countryCode(),
                coupon.creationDate());
    }
}
