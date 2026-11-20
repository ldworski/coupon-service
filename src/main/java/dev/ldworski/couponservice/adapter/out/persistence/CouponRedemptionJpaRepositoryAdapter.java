package dev.ldworski.couponservice.adapter.out.persistence;

import dev.ldworski.couponservice.domain.exception.CouponAlreadyRedeemedException;
import dev.ldworski.couponservice.domain.model.CouponRedemption;
import dev.ldworski.couponservice.domain.port.out.CouponRedemptionRepositoryPort;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class CouponRedemptionJpaRepositoryAdapter implements CouponRedemptionRepositoryPort {

    private static final String PRIMARY_KEY_CONSTRAINT = "pk_coupon_redemption";

    private final CouponRedemptionEntityJpaRepository couponRedemptionEntityJpaRepository;
    private final CouponRedemptionPersistenceMapper couponRedemptionPersistenceMapper;

    public CouponRedemptionJpaRepositoryAdapter(
            CouponRedemptionEntityJpaRepository couponRedemptionEntityJpaRepository,
            CouponRedemptionPersistenceMapper couponRedemptionPersistenceMapper) {
        this.couponRedemptionEntityJpaRepository = couponRedemptionEntityJpaRepository;
        this.couponRedemptionPersistenceMapper = couponRedemptionPersistenceMapper;
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
        return Optional.ofNullable(violation.getConstraintName())
                        .map(value -> value.contains(PRIMARY_KEY_CONSTRAINT))
                        .orElse(Boolean.FALSE)
                && violation.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE;
    }
}
