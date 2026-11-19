package dev.ldworski.couponservice.adapter.out.persistence;

import dev.ldworski.couponservice.domain.model.Coupon;
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class CouponJpaRepositoryAdapter implements CouponRepositoryPort {

    private final CouponEntityJpaRepository couponEntityJpaRepository;
    private final CouponPersistenceMapper couponPersistenceMapper;

    public CouponJpaRepositoryAdapter(
            CouponEntityJpaRepository couponEntityJpaRepository, CouponPersistenceMapper couponPersistenceMapper) {
        this.couponEntityJpaRepository = couponEntityJpaRepository;
        this.couponPersistenceMapper = couponPersistenceMapper;
    }

    @Override
    public Coupon registerCoupon(Coupon coupon) {
        var couponEntity = couponPersistenceMapper.from(coupon);
        var registeredCouponEntity = couponEntityJpaRepository.save(couponEntity);
        return couponPersistenceMapper.from(registeredCouponEntity);
    }
}
