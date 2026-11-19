package dev.ldworski.couponservice

import dev.ldworski.couponservice.configuration.TestcontainersConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import spock.lang.Specification

@SpringBootTest
@Import(TestcontainersConfiguration)
class CouponServiceApplicationIT extends Specification {

    @Autowired
    ApplicationContext context

    def "should load application context"() {
        expect:
        context != null
    }
}
