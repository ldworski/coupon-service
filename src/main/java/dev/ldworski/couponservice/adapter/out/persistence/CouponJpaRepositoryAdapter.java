package dev.ldworski.couponservice.adapter.out.persistence;

import static dev.ldworski.couponservice.configuration.CacheConfiguration.EXHAUSTED_COUPONS_CACHE;

import dev.ldworski.couponservice.domain.exception.CouponCodeAlreadyExistsException;
import dev.ldworski.couponservice.domain.model.CountryCode;
import dev.ldworski.couponservice.domain.model.Coupon;
import dev.ldworski.couponservice.domain.model.CouponCode;
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.cache.annotation.Cacheable;
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
                    throw new CouponCodeAlreadyExistsException(coupon.code(), exception);
                case null, default -> throw exception;
            }
        }
    }

    @Override
    public Optional<Coupon> findByCodeAndCountryCode(CouponCode code, CountryCode countryCode) {
        return couponEntityJpaRepository
                .findByCodeAndCountryCode(code.value(), countryCode)
                .map(couponPersistenceMapper::from);
    }

    @Override
    @Cacheable(cacheNames = EXHAUSTED_COUPONS_CACHE, unless = "#result == null or !#result.usageLimitReached")
    public Optional<Coupon> findByCode(CouponCode code) {
        return couponEntityJpaRepository.findByCode(code.value()).map(couponPersistenceMapper::from);
    }

    @Override
    public Optional<Coupon> findByCodeForUpdate(CouponCode code) {
        return couponEntityJpaRepository.findWithLockByCode(code.value()).map(couponPersistenceMapper::from);
    }

    @Override
    public void updateUsage(Coupon coupon) {
        var couponEntity =
                couponEntityJpaRepository.findById(coupon.id().value()).orElseThrow();
        couponEntity.setCurrentUsage(coupon.usage().current());
        couponEntityJpaRepository.saveAndFlush(couponEntity);
    }

    private boolean isCouponCodeDuplicated(ConstraintViolationException violation) {
        return "uq_coupon_code".equals(violation.getConstraintName());
    }
}
