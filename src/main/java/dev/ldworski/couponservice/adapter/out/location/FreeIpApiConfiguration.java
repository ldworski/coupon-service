package dev.ldworski.couponservice.adapter.out.location;

import dev.ldworski.couponservice.domain.port.out.CountryIdentificationPort;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FreeIpApiProperties.class)
class FreeIpApiConfiguration {

    @Bean
    CountryIdentificationPort countryIdentificationPort(FreeIpApiProperties properties) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        var restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        return new CountryIdentificationAdapter(restClient, retryTemplate(properties.retry()));
    }

    private static RetryTemplate retryTemplate(FreeIpApiProperties.Retry retry) {
        var retryPolicy = RetryPolicy.builder()
                .maxRetries(retry.maxRetries())
                .delay(retry.delay())
                .multiplier(retry.multiplier())
                .jitter(retry.jitter())
                .maxDelay(retry.maxDelay())
                .includes(HttpServerErrorException.class, ResourceAccessException.class)
                .build();
        return new RetryTemplate(retryPolicy);
    }
}
