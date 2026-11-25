package dev.ldworski.couponservice.domain.model

import spock.lang.Specification

class UserIdSpec extends Specification {

    def "should create user id from UUID"() {
        given:
        def uuid = UUID.randomUUID()

        expect:
        new UserId(uuid).value() == uuid
    }

    def "should reject null value"() {
        when:
        new UserId(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "value must not be null"
    }
}
