package dev.ldworski.couponservice.adapter.out.persistence;

import dev.ldworski.couponservice.domain.exception.CouponCodeAlreadyExistsException;
import dev.ldworski.couponservice.domain.model.Coupon;
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            var registeredCouponEntity = couponEntityJpaRepository.saveAndFlush(couponEntity);
            return couponPersistenceMapper.from(registeredCouponEntity);
        } catch (DataIntegrityViolationException exception) {
            switch (exception.getCause()) {
                case ConstraintViolationException violation
                when isCouponCodeDuplicated(violation) ->
                    throw new CouponCodeAlreadyExistsException(
                            coupon.getCouponDefinition().code(), exception);
                case null, default -> throw exception;
            }
        }
    }

    private boolean isCouponCodeDuplicated(ConstraintViolationException violation) {
        return "uq_coupon_code".equals(violation.getConstraintName());
    }
}
