package dev.ldworski.couponservice.adapter.out.location

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.serverError
import static com.github.tomakehurst.wiremock.client.WireMock.status
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.http.Fault
import dev.ldworski.couponservice.configuration.CacheConfiguration
import dev.ldworski.couponservice.domain.model.CountryCode
import dev.ldworski.couponservice.domain.model.IpAddress
import dev.ldworski.couponservice.domain.port.out.CountryIdentificationPort
import java.nio.charset.StandardCharsets
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import spock.lang.Specification

@SpringBootTest(
        classes = [FreeIpApiConfiguration, CacheConfiguration],
        properties = [
                "coupon-service.free-ip-api.retry.delay=10ms",
                "coupon-service.free-ip-api.retry.jitter=5ms",
                "coupon-service.free-ip-api.retry.max-delay=100ms"
        ])
@ImportAutoConfiguration(CacheAutoConfiguration)
class CountryIdentificationAdapterIT extends Specification {

    static final WireMockServer wireMock = new WireMockServer(options().dynamicPort())

    static {
        wireMock.start()
    }

    @DynamicPropertySource
    static void freeIpApiProperties(DynamicPropertyRegistry registry) {
        registry.add("coupon-service.free-ip-api.base-url", wireMock::baseUrl)
    }

    @Autowired
    CountryIdentificationPort countryIdentificationPort

    @Autowired
    CacheManager cacheManager

    def setup() {
        wireMock.resetAll()
        cacheManager.getCache(CacheConfiguration.IP_COUNTRIES_CACHE).clear()
    }

    def cleanupSpec() {
        wireMock.stop()
    }

    def "should identify supported country #expected for IP #ip"() {
        given:
        stubFor(path, okJson(freeIpApiResponse(response)))

        when:
        def result = countryIdentificationPort.identifyCountry(new IpAddress(ip))

        then:
        result == Optional.of(expected)
        requestCount(path) == 1

        where:
        ip             | path                    | response                  || expected
        "83.0.0.1"     | "83.0.0.1"              | "identified-pl-ipv4.json" || CountryCode.PL
        "2a02:8108::1" | "2a02:8108::1"          | "identified-de-ipv6.json" || CountryCode.DE
    }

    def "should return empty when country is not supported"() {
        given:
        stubFor("8.8.8.8", okJson(freeIpApiResponse("identified-us-ipv4.json")))

        expect:
        countryIdentificationPort.identifyCountry(new IpAddress("8.8.8.8")).isEmpty()
    }

    def "should return empty when country is not identified for #description IP"() {
        given:
        stubFor(path, okJson(freeIpApiResponse("not-identified.json")))

        expect:
        countryIdentificationPort.identifyCountry(new IpAddress(ip)).isEmpty()

        where:
        description     | ip          | path
        "private"       | "10.0.0.1"  | "10.0.0.1"
        "loopback"      | "127.0.0.1" | "127.0.0.1"
        "IPv6 loopback" | "::1"       | "::1"
    }

    def "should return empty when response body is empty"() {
        given:
        stubFor("83.0.0.1", aResponse().withStatus(200))

        expect:
        countryIdentificationPort.identifyCountry(new IpAddress("83.0.0.1")).isEmpty()
    }

    def "should not retry when rate limit is exceeded"() {
        given:
        stubFor("83.0.0.1", status(429))

        when:
        countryIdentificationPort.identifyCountry(new IpAddress("83.0.0.1"))

        then:
        thrown(HttpClientErrorException.TooManyRequests)
        requestCount("83.0.0.1") == 1
    }

    def "should identify country after retrying when Free IP API is temporarily unavailable"() {
        given:
        // WireMock scenario: each request returns the stub for the current state and moves to the next one
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/json/83.0.0.1")).inScenario("retries")
                .whenScenarioStateIs(STARTED)
                .willReturn(serverError())
                .willSetStateTo("second attempt"))
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/json/83.0.0.1")).inScenario("retries")
                .whenScenarioStateIs("second attempt")
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("third attempt"))
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/json/83.0.0.1")).inScenario("retries")
                .whenScenarioStateIs("third attempt")
                .willReturn(status(503))
                .willSetStateTo("fourth attempt"))
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/json/83.0.0.1")).inScenario("retries")
                .whenScenarioStateIs("fourth attempt")
                .willReturn(okJson(freeIpApiResponse("identified-pl-ipv4.json"))))

        when:
        def result = countryIdentificationPort.identifyCountry(new IpAddress("83.0.0.1"))

        then:
        result == Optional.of(CountryCode.PL)
        requestCount("83.0.0.1") == 4
    }

    def "should propagate last error when Free IP API is unavailable after 3 retries due to #description"() {
        given:
        stubFor("83.0.0.1", response)

        when:
        countryIdentificationPort.identifyCountry(new IpAddress("83.0.0.1"))

        then:
        thrown(expected)
        requestCount("83.0.0.1") == 4

        where:
        description          | response                                                || expected
        "server error"       | serverError()                                           || HttpServerErrorException
        "connection failure" | aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)   || ResourceAccessException
    }

    def "should call Free IP API only once for repeated requests from the same IP"() {
        given:
        stubFor("83.0.0.1", okJson(freeIpApiResponse("identified-pl-ipv4.json")))

        when:
        def first = countryIdentificationPort.identifyCountry(new IpAddress("83.0.0.1"))
        def second = countryIdentificationPort.identifyCountry(new IpAddress("83.0.0.1"))

        then:
        first == Optional.of(CountryCode.PL)
        second == Optional.of(CountryCode.PL)
        requestCount("83.0.0.1") == 1
    }

    def "should cache not identified country"() {
        given:
        stubFor("10.0.0.1", okJson(freeIpApiResponse("not-identified.json")))

        when:
        def first = countryIdentificationPort.identifyCountry(new IpAddress("10.0.0.1"))
        def second = countryIdentificationPort.identifyCountry(new IpAddress("10.0.0.1"))

        then:
        first.isEmpty()
        second.isEmpty()
        requestCount("10.0.0.1") == 1
    }

    def "should call Free IP API for each different IP"() {
        given:
        stubFor("83.0.0.1", okJson(freeIpApiResponse("identified-pl-ipv4.json")))
        stubFor("8.8.8.8", okJson(freeIpApiResponse("identified-us-ipv4.json")))

        when:
        def poland = countryIdentificationPort.identifyCountry(new IpAddress("83.0.0.1"))
        def unitedStates = countryIdentificationPort.identifyCountry(new IpAddress("8.8.8.8"))

        then:
        poland == Optional.of(CountryCode.PL)
        unitedStates.isEmpty()
        requestCount("83.0.0.1") == 1
        requestCount("8.8.8.8") == 1
    }

    def "should not cache failure when Free IP API is unavailable"() {
        given:
        stubFor("83.0.0.1", serverError())

        when:
        countryIdentificationPort.identifyCountry(new IpAddress("83.0.0.1"))

        then:
        thrown(HttpServerErrorException)

        when: "Free IP API is available again - the most recently added stub takes precedence"
        stubFor("83.0.0.1", okJson(freeIpApiResponse("identified-pl-ipv4.json")))
        def result = countryIdentificationPort.identifyCountry(new IpAddress("83.0.0.1"))

        then:
        result == Optional.of(CountryCode.PL)
        requestCount("83.0.0.1") == 5
    }

    private static void stubFor(String ip, ResponseDefinitionBuilder response) {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/json/$ip")).willReturn(response))
    }

    private static int requestCount(String ip) {
        wireMock.countRequestsMatching(getRequestedFor(urlPathEqualTo("/api/v1/json/$ip")).build()).count
    }

    private static String freeIpApiResponse(String fileName) {
        new ClassPathResource("free-ip-api/$fileName").getContentAsString(StandardCharsets.UTF_8)
    }
}
