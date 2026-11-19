package dev.ldworski.couponservice.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponEntityJpaRepository extends CrudRepository<CouponEntity, UUID> {}
