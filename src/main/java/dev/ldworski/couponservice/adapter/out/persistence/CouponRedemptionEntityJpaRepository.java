package dev.ldworski.couponservice.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRedemptionEntityJpaRepository
        extends JpaRepository<CouponRedemptionEntity, CouponRedemptionId> {}
