package dev.ldworski.couponservice

import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("test")
abstract class IntegrationTestSpecification extends Specification {
}
