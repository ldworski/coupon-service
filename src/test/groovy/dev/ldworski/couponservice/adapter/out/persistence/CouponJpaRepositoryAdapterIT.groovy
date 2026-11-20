package dev.ldworski.couponservice.adapter.out.persistence

import dev.ldworski.couponservice.configuration.TestcontainersConfiguration
import dev.ldworski.couponservice.domain.model.CountryCode
import dev.ldworski.couponservice.domain.model.Coupon
import dev.ldworski.couponservice.domain.model.CouponCode
import dev.ldworski.couponservice.domain.model.CouponDefinition
import dev.ldworski.couponservice.domain.exception.CouponCodeAlreadyExistsException
import jakarta.persistence.EntityManager
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import spock.lang.Specification

import java.time.Instant

@DataJpaTest
@Import([TestcontainersConfiguration, CouponJpaRepositoryAdapter, CouponPersistenceMapper])
class CouponJpaRepositoryAdapterIT extends Specification {

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
        registered.couponDefinition.code().value() == CODE
        registered.couponDefinition.maxAllowedUsage() == MAX_ALLOWED_USAGE
        registered.couponDefinition.countryCode() == COUNTRY_CODE
        registered.currentUsages == 0
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
            couponDefinition == coupon.couponDefinition
            currentUsages == 0
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
        Coupon.newCoupon(new CouponDefinition(new CouponCode(code), MAX_ALLOWED_USAGE, COUNTRY_CODE), CREATION_DATE)
    }
}
