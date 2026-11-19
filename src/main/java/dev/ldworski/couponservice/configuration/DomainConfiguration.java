package dev.ldworski.couponservice.configuration;

import dev.ldworski.couponservice.domain.port.in.RegisterCouponUseCase;
import dev.ldworski.couponservice.domain.port.out.CouponRepositoryPort;
import dev.ldworski.couponservice.domain.service.RegisterCouponService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RegisterCouponUseCase registerCouponUseCase(CouponRepositoryPort couponRepositoryPort, Clock clock) {
        return new RegisterCouponService(couponRepositoryPort, clock);
    }
}
