package dev.ldworski.couponservice.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponEntityJpaRepository extends JpaRepository<CouponEntity, UUID> {}
