package dev.ldworski.couponservice.domain.model

import spock.lang.Specification

class IpAddressSpec extends Specification {

    def "should accept #description '#value'"() {
        when:
        def ipAddress = new IpAddress(value)

        then:
        ipAddress.value() == value

        where:
        description                | value
        "IPv4 address"             | "192.168.0.1"
        "lowest IPv4 address"      | "0.0.0.0"
        "highest IPv4 address"     | "255.255.255.255"
        "IPv4 loopback"            | "127.0.0.1"
        "full IPv6 address"        | "2001:0db8:0000:0000:0000:0000:0000:0001"
        "compressed IPv6 address"  | "2001:db8::1"
        "upper case IPv6 address"  | "2001:DB8::1"
        "IPv6 loopback"            | "::1"
        "IPv6 unspecified address" | "::"
        "IPv4-mapped IPv6 address" | "::ffff:192.168.0.1"
    }

    def "should be equal for the same address"() {
        expect:
        new IpAddress("192.168.0.1") == new IpAddress("192.168.0.1")
        new IpAddress("192.168.0.1").hashCode() == new IpAddress("192.168.0.1").hashCode()
    }

    def "should not be equal for different addresses: '#first' and '#second'"() {
        expect:
        new IpAddress(first) != new IpAddress(second)

        where:
        first         | second
        "192.168.0.1" | "192.168.0.2"
        "::1"         | "127.0.0.1"
        "2001:db8::1" | "2001:db8::2"
    }

    def "should reject null value"() {
        when:
        new IpAddress(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "value must not be null"
    }

    def "should reject value when #description"() {
        when:
        new IpAddress(value)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "value must be a valid IPv4 or IPv6 address"

        where:
        description                      | value
        "it is empty"                    | ""
        "IPv4 is in legacy short form"   | "127.1"
        "it is whitespace only"          | "   "
        "it has leading whitespace"      | " 192.168.0.1"
        "it has trailing whitespace"     | "192.168.0.1 "
        "it is a hostname"               | "localhost"
        "it is a domain name"            | "example.com"
        "IPv4 octet is out of range"     | "256.1.1.1"
        "IPv4 has too many octets"       | "1.2.3.4.5"
        "IPv4 has trailing dot"          | "1.2.3."
        "IPv4 octet is hexadecimal"      | "0x7f.0.0.1"
        "IPv4 is negative"               | "-1.0.0.0"
        "IPv4 contains non-ASCII digits" | "١.٢.٣.٤"
        "IPv4 has port"                  | "192.168.0.1:8080"
        "IPv4 is a CIDR block"           | "192.168.0.0/24"
        "it is a comma separated list"   | "1.2.3.4, 10.0.0.1"
        "IPv6 has too many groups"       | "1:2:3:4:5:6:7:8:9"
        "IPv6 has double compression"    | "2001::db8::1"
        "IPv6 has invalid hex digit"     | "2001:db8::g"
    }
}
