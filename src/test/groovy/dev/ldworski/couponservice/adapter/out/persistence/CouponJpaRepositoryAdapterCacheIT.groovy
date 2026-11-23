package dev.ldworski.couponservice.adapter.out.persistence

import dev.ldworski.couponservice.configuration.CacheConfiguration
import dev.ldworski.couponservice.configuration.TestcontainersConfiguration
import dev.ldworski.couponservice.domain.model.CountryCode
import dev.ldworski.couponservice.domain.model.Coupon
import dev.ldworski.couponservice.domain.model.CouponCode
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import dev.ldworski.couponservice.IntegrationTestSpecification

import java.time.Instant

@DataJpaTest
@Import([TestcontainersConfiguration, CouponJpaRepositoryAdapter, CouponPersistenceMapper, CacheConfiguration])
@ImportAutoConfiguration(CacheAutoConfiguration)
class CouponJpaRepositoryAdapterCacheIT extends IntegrationTestSpecification {

    private static final CouponCode CODE = new CouponCode("WINTER2026")

    @Autowired
    CouponRepositoryPort couponRepositoryPort

    @Autowired
    CacheManager cacheManager

    def setup() {
        exhaustedCouponsCache().clear()
    }

    def "should cache coupon found by code when its usage limit is reached"() {
        given:
        def coupon = couponRepositoryPort.registerCoupon(newCoupon(1))
        couponRepositoryPort.updateUsage(coupon.tryToRedeem())

        when:
        def found = couponRepositoryPort.findByCode(CODE)

        then:
        found.get().usageLimitReached
        (exhaustedCouponsCache().get(CODE)?.get() as Coupon)?.id == coupon.id
    }

    def "should not cache coupon found by code when its usage limit is not reached"() {
        given:
        couponRepositoryPort.registerCoupon(newCoupon(2))

        when:
        def found = couponRepositoryPort.findByCode(CODE)

        then:
        found.isPresent()
        exhaustedCouponsCache().get(CODE) == null
    }

    def "should not cache missing coupon"() {
        when:
        def found = couponRepositoryPort.findByCode(CODE)

        then:
        found.isEmpty()
        exhaustedCouponsCache().get(CODE) == null
    }

    def "should not cache coupon found for update"() {
        given:
        def coupon = couponRepositoryPort.registerCoupon(newCoupon(1))
        couponRepositoryPort.updateUsage(coupon.tryToRedeem())

        when:
        couponRepositoryPort.findByCodeForUpdate(CODE)

        then:
        exhaustedCouponsCache().get(CODE) == null
    }

    private Cache exhaustedCouponsCache() {
        cacheManager.getCache(CacheConfiguration.EXHAUSTED_COUPONS_CACHE)
    }

    private static Coupon newCoupon(int maxAllowedUsage) {
        Coupon.newCoupon(CODE, CountryCode.PL, maxAllowedUsage, Instant.now())
    }
}
