package dev.ldworski.couponservice.adapter.out.persistence;

import dev.ldworski.couponservice.domain.model.Coupon;
import dev.ldworski.couponservice.domain.model.CouponCode;
import dev.ldworski.couponservice.domain.model.CouponDefinition;
import dev.ldworski.couponservice.domain.model.CouponId;
import org.springframework.stereotype.Component;

@Component
public class CouponPersistenceMapper {

    public Coupon from(CouponEntity couponEntity) {
        return new Coupon(
                new CouponId(couponEntity.getId()),
                new CouponDefinition(
                        new CouponCode(couponEntity.getCode()),
                        couponEntity.getMaxAllowedUsage(),
                        couponEntity.getCountryCode()),
                couponEntity.getCurrentUsage(),
                couponEntity.getCreated());
    }

    public CouponEntity from(Coupon coupon) {
        return new CouponEntity(
                coupon.getId().value(),
                coupon.getCouponDefinition().code().value(),
                coupon.getCouponDefinition().maxAllowedUsage(),
                coupon.getCurrentUsages(),
                coupon.getCouponDefinition().countryCode(),
                coupon.getCreationDate());
    }
}
