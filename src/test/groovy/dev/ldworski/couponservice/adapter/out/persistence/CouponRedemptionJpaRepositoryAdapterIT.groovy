package dev.ldworski.couponservice.adapter.out.persistence

import dev.ldworski.couponservice.configuration.TestcontainersConfiguration
import dev.ldworski.couponservice.domain.exception.CouponAlreadyRedeemedException
import dev.ldworski.couponservice.domain.model.CountryCode
import dev.ldworski.couponservice.domain.model.Coupon
import dev.ldworski.couponservice.domain.model.CouponCode
import dev.ldworski.couponservice.domain.model.CouponDefinition
import dev.ldworski.couponservice.domain.model.CouponId
import dev.ldworski.couponservice.domain.model.CouponRedemption
import dev.ldworski.couponservice.domain.model.UserId
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import dev.ldworski.couponservice.IntegrationTestSpecification

import java.time.Instant

@DataJpaTest
@Import([TestcontainersConfiguration, CouponRedemptionJpaRepositoryAdapter, CouponRedemptionPersistenceMapper,
        CouponJpaRepositoryAdapter, CouponPersistenceMapper])
class CouponRedemptionJpaRepositoryAdapterIT extends IntegrationTestSpecification {

    private static final Instant REDEMPTION_DATE = Instant.parse("2026-11-20T10:15:30.123456Z")
    private static final UserId USER_ID = new UserId(UUID.randomUUID())

    @Autowired
    CouponRedemptionJpaRepositoryAdapter adapter

    @Autowired
    CouponJpaRepositoryAdapter couponAdapter

    @Autowired
    CouponRedemptionEntityJpaRepository couponRedemptionEntityJpaRepository

    @Autowired
    EntityManager entityManager

    def "should persist coupon redemption in database"() {
        given:
        def coupon = registeredCoupon("WINTER2026")

        when:
        adapter.registerRedemption(new CouponRedemption(coupon.id, USER_ID, REDEMPTION_DATE))
        entityManager.clear()

        then:
        def stored = couponRedemptionEntityJpaRepository.findById(new CouponRedemptionId(coupon.id.value(), USER_ID.value())).orElseThrow()
        stored.created == REDEMPTION_DATE
    }

    def "should reject second redemption of the same coupon by the same user"() {
        given:
        def coupon = registeredCoupon("WINTER2026")
        adapter.registerRedemption(new CouponRedemption(coupon.id, USER_ID, REDEMPTION_DATE))
        entityManager.clear()

        when:
        adapter.registerRedemption(new CouponRedemption(coupon.id, USER_ID, REDEMPTION_DATE.plusSeconds(60)))

        then:
        def exception = thrown(CouponAlreadyRedeemedException)
        exception.couponId == coupon.id
        exception.userId == USER_ID
    }

    def "should allow redemption of the same coupon by different users"() {
        given:
        def coupon = registeredCoupon("WINTER2026")
        def otherUserId = new UserId(UUID.randomUUID())

        when:
        adapter.registerRedemption(new CouponRedemption(coupon.id, USER_ID, REDEMPTION_DATE))
        adapter.registerRedemption(new CouponRedemption(coupon.id, otherUserId, REDEMPTION_DATE))

        then:
        couponRedemptionEntityJpaRepository.count() == 2
    }

    def "should allow redemption of different coupons by the same user"() {
        given:
        def winter = registeredCoupon("WINTER2026")
        def summer = registeredCoupon("SUMMER2026")

        when:
        adapter.registerRedemption(new CouponRedemption(winter.id, USER_ID, REDEMPTION_DATE))
        adapter.registerRedemption(new CouponRedemption(summer.id, USER_ID, REDEMPTION_DATE))

        then:
        couponRedemptionEntityJpaRepository.count() == 2
    }

    def "should reject redemption of not existing coupon"() {
        when:
        adapter.registerRedemption(new CouponRedemption(CouponId.newCouponId(), USER_ID, REDEMPTION_DATE))

        then:
        def exception = thrown(DataIntegrityViolationException)
        exception.message.contains("fk_coupon_redemption_coupon")
    }

    private Coupon registeredCoupon(String code) {
        couponAdapter.registerCoupon(
                Coupon.newCoupon(new CouponDefinition(new CouponCode(code), 100, CountryCode.PL), Instant.now()))
    }
}
