package dev.ldworski.couponservice.adapter.out.persistence;

import dev.ldworski.couponservice.domain.model.CouponRedemption;
import org.springframework.stereotype.Component;

@Component
public class CouponRedemptionPersistenceMapper {

    public CouponRedemptionEntity from(CouponRedemption couponRedemption) {
        return new CouponRedemptionEntity(
                new CouponRedemptionId(
                        couponRedemption.couponId().value(),
                        couponRedemption.userId().value()),
                couponRedemption.redemptionDate());
    }
}
