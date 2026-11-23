package dev.ldworski.couponservice.adapter.out.persistence

import dev.ldworski.couponservice.configuration.TestcontainersConfiguration
import dev.ldworski.couponservice.domain.model.CountryCode
import dev.ldworski.couponservice.domain.model.Coupon
import dev.ldworski.couponservice.domain.model.CouponCode
import dev.ldworski.couponservice.domain.exception.CouponCodeAlreadyExistsException
import jakarta.persistence.EntityManager
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import dev.ldworski.couponservice.IntegrationTestSpecification

import java.time.Instant

@DataJpaTest
@Import([TestcontainersConfiguration, CouponJpaRepositoryAdapter, CouponPersistenceMapper])
class CouponJpaRepositoryAdapterIT extends IntegrationTestSpecification {

    private static final Instant CREATION_DATE = Instant.parse("2026-11-19T10:15:30.123456Z")
    private static final String CODE = "WINTER2026"
    private static final int MAX_ALLOWED_USAGE = 100
    private static final CountryCode COUNTRY_CODE = CountryCode.PL

    @Autowired
    CouponJpaRepositoryAdapter adapter

    @Autowired
    CouponEntityJpaRepository couponEntityJpaRepository

    @Autowired
    EntityManager entityManager

    def "should return registered coupon"() {
        given:
        def coupon = coupon(CODE)

        when:
        def registered = adapter.registerCoupon(coupon)

        then:
        registered.id == coupon.id
        registered.code.value() == CODE
        registered.usage.maxAllowed() == MAX_ALLOWED_USAGE
        registered.countryCode == COUNTRY_CODE
        registered.usage.current() == 0
        registered.creationDate == CREATION_DATE
    }

    def "should persist coupon in database"() {
        given:
        def coupon = coupon(CODE)

        when:
        adapter.registerCoupon(coupon)
        entityManager.flush()
        entityManager.clear()

        then:
        def stored = couponEntityJpaRepository.findById(coupon.id.value()).orElseThrow()
        stored.code == CODE
        stored.maxAllowedUsage == MAX_ALLOWED_USAGE
        stored.currentUsage == 0
        stored.countryCode == COUNTRY_CODE
        stored.created == CREATION_DATE
    }

    def "should reject duplicated coupon code"() {
        given:
        adapter.registerCoupon(coupon(CODE))

        when:
        adapter.registerCoupon(coupon(CODE))

        then:
        def exception = thrown(CouponCodeAlreadyExistsException)
        exception.code == new CouponCode(CODE)
    }

    def "should find coupon by code and country code"() {
        given:
        def coupon = adapter.registerCoupon(coupon(CODE))
        entityManager.clear()

        when:
        def found = adapter.findByCodeAndCountryCode(new CouponCode(CODE), COUNTRY_CODE)

        then:
        found.isPresent()
        with(found.get()) {
            id == coupon.id
            code == coupon.code
            countryCode == coupon.countryCode
            usage == coupon.usage
            creationDate == CREATION_DATE
        }
    }

    def "should not find coupon when #description"() {
        given:
        adapter.registerCoupon(coupon(CODE))

        expect:
        adapter.findByCodeAndCountryCode(new CouponCode(code), countryCode).isEmpty()

        where:
        description                          | code         | countryCode
        "code does not exist"                | "SUMMER2026" | COUNTRY_CODE
        "coupon is defined for other country" | CODE         | CountryCode.DE
    }

    def "should find coupon by code"() {
        given:
        def coupon = adapter.registerCoupon(coupon(CODE))
        entityManager.clear()

        when:
        def found = adapter.findByCode(new CouponCode(CODE))

        then:
        found.map { it.id } == Optional.of(coupon.id)
    }

    def "should not find coupon by code when code does not exist"() {
        given:
        adapter.registerCoupon(coupon(CODE))

        expect:
        adapter.findByCode(new CouponCode("SUMMER2026")).isEmpty()
    }

    def "should find coupon for update by code"() {
        given:
        def coupon = adapter.registerCoupon(coupon(CODE))
        entityManager.clear()

        when:
        def found = adapter.findByCodeForUpdate(new CouponCode(CODE))

        then:
        found.map { it.id } == Optional.of(coupon.id)
    }

    def "should not find coupon for update when code does not exist"() {
        given:
        adapter.registerCoupon(coupon(CODE))

        expect:
        adapter.findByCodeForUpdate(new CouponCode("SUMMER2026")).isEmpty()
    }

    def "should update coupon usage"() {
        given:
        def coupon = adapter.registerCoupon(coupon(CODE))

        when:
        adapter.updateUsage(coupon.tryToRedeem())
        entityManager.clear()

        then:
        couponEntityJpaRepository.findById(coupon.id.value()).orElseThrow().currentUsage == 1
    }

    def "should reject current usage exceeding max allowed usage on database level"() {
        given:
        def coupon = adapter.registerCoupon(coupon(CODE))

        when:
        entityManager.createNativeQuery("UPDATE coupon SET current_usage = max_allowed_usage + 1 WHERE id = :id")
                .setParameter("id", coupon.id.value())
                .executeUpdate()

        then:
        def exception = thrown(ConstraintViolationException)
        exception.constraintName == "ck_coupon_current_usage_within_limit"
    }

    def "should reject code with invalid format on database level when #description"() {
        when:
        entityManager.createNativeQuery("""
                INSERT INTO coupon (id, code, current_usage, max_allowed_usage, country_code, created)
                VALUES (:id, :code, 0, 100, 'PL', :created)
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("code", code)
                .setParameter("created", CREATION_DATE)
                .executeUpdate()

        then:
        def exception = thrown(ConstraintViolationException)
        exception.message.contains("ck_coupon_code_format")

        where:
        description                | code
        "code is empty"            | ""
        "code is lower case"       | "winter2026"
        "code contains whitespace" | "WINTER 2026"
        "code contains new line"   | "WINTER\n2026"
        "code contains dash"       | "WINTER-2026"
        "code contains Polish letter" | "ZIMAŚ2026"
    }

    private static Coupon coupon(String code) {
        Coupon.newCoupon(new CouponCode(code), COUNTRY_CODE, MAX_ALLOWED_USAGE, CREATION_DATE)
    }
}
