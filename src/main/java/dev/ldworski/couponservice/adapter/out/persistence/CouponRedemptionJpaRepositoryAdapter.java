package dev.ldworski.couponservice.adapter.out.persistence;

import dev.ldworski.couponservice.domain.exception.CouponAlreadyRedeemedException;
import dev.ldworski.couponservice.domain.model.CouponId;
import dev.ldworski.couponservice.domain.model.CouponRedemption;
import dev.ldworski.couponservice.domain.model.UserId;
import dev.ldworski.couponservice.domain.port.out.CouponRedemptionRepositoryPort;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class CouponRedemptionJpaRepositoryAdapter implements CouponRedemptionRepositoryPort {

    private final CouponRedemptionEntityJpaRepository couponRedemptionEntityJpaRepository;
    private final CouponRedemptionPersistenceMapper couponRedemptionPersistenceMapper;

    public CouponRedemptionJpaRepositoryAdapter(
            CouponRedemptionEntityJpaRepository couponRedemptionEntityJpaRepository,
            CouponRedemptionPersistenceMapper couponRedemptionPersistenceMapper) {
        this.couponRedemptionEntityJpaRepository = couponRedemptionEntityJpaRepository;
        this.couponRedemptionPersistenceMapper = couponRedemptionPersistenceMapper;
    }

    @Override
    public boolean isRedeemed(CouponId couponId, UserId userId) {
        return couponRedemptionEntityJpaRepository.existsById(new CouponRedemptionId(couponId.value(), userId.value()));
    }

    @Override
    public void registerRedemption(CouponRedemption couponRedemption) {
        var couponRedemptionEntity = couponRedemptionPersistenceMapper.from(couponRedemption);
        try {
            couponRedemptionEntityJpaRepository.saveAndFlush(couponRedemptionEntity);
        } catch (DataIntegrityViolationException exception) {
            switch (exception.getCause()) {
                case ConstraintViolationException violation
                when isAlreadyRedeemed(violation) ->
                    throw new CouponAlreadyRedeemedException(
                            couponRedemption.couponId(), couponRedemption.userId(), exception);
                case null, default -> throw exception;
            }
        }
    }

    private boolean isAlreadyRedeemed(ConstraintViolationException violation) {
        return "pk_coupon_redemption".equals(violation.getConstraintName());
    }
}
