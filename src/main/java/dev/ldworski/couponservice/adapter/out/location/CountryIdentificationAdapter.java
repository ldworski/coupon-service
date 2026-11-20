package dev.ldworski.couponservice.adapter.out.location;

import dev.ldworski.couponservice.domain.model.CountryCode;
import dev.ldworski.couponservice.domain.model.IpAddress;
import dev.ldworski.couponservice.domain.port.out.CountryIdentificationPort;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.web.client.RestClient;

public class CountryIdentificationAdapter implements CountryIdentificationPort {

    static final String CACHE_NAME = "ip-countries";
    private static final String GET_IP_LOCATION_PATH = "/api/v1/json";
    private final RestClient freeIpApiRestClient;
    private final RetryTemplate freeIpApiRetryTemplate;

    public CountryIdentificationAdapter(RestClient freeIpApiRestClient, RetryTemplate freeIpApiRetryTemplate) {
        this.freeIpApiRestClient = freeIpApiRestClient;
        this.freeIpApiRetryTemplate = freeIpApiRetryTemplate;
    }

    @Override
    @Cacheable(CACHE_NAME)
    public Optional<CountryCode> identifyCountry(IpAddress ipAddress) {
        var response = withRetries(() -> freeIpApiRestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(GET_IP_LOCATION_PATH)
                        .pathSegment(ipAddress.getValue())
                        .build())
                .retrieve()
                .body(FreeIpApiResponse.class));

        return Optional.ofNullable(response)
                .map(FreeIpApiResponse::countryCode)
                .flatMap(this::toCountryCode);
    }

    private FreeIpApiResponse withRetries(Retryable<FreeIpApiResponse> call) {
        try {
            return freeIpApiRetryTemplate.execute(call);
        } catch (RetryException exception) {
            // TODO opakuj w wyjątek domenowy
            if (exception.getCause() instanceof RuntimeException lastFailure) {
                throw lastFailure;
            }
            throw new IllegalStateException("Free IP API call failed", exception.getCause());
        }
    }

    private Optional<CountryCode> toCountryCode(String countryCode) {
        return Arrays.stream(CountryCode.values())
                .filter(supported -> supported.name().equals(countryCode))
                .findFirst();
    }
}
