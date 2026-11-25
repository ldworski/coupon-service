package dev.ldworski.couponservice.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfiguration {

    public static final String IP_COUNTRIES_CACHE = "ip-countries";
    public static final String EXHAUSTED_COUPONS_CACHE = "exhausted-coupons";

    @Bean
    CacheManager cacheManager(CacheProperties properties) {
        var cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of());
        cacheManager.registerCustomCache(IP_COUNTRIES_CACHE, caffeineCache(properties.ipCountries()));
        cacheManager.registerCustomCache(EXHAUSTED_COUPONS_CACHE, caffeineCache(properties.exhaustedCoupons()));
        return cacheManager;
    }

    private Cache<Object, Object> caffeineCache(CacheProperties.CacheSettings properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.maximumSize())
                .expireAfterWrite(properties.expireAfterWrite())
                .recordStats()
                .build();
    }
}
