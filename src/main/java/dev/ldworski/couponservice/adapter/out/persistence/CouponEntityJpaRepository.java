package dev.ldworski.couponservice.adapter.out.persistence;

import dev.ldworski.couponservice.domain.model.CountryCode;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponEntityJpaRepository extends JpaRepository<CouponEntity, UUID> {

    Optional<CouponEntity> findByCodeAndCountryCode(String code, CountryCode countryCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CouponEntity> findWithLockByCode(String code);
}
