package com.goormgb.be.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    // ========== 1. AUTH-GUARD METRICS (:8080) ==========

    @Bean
    public Counter authAttemptsCounter() {
        return Counter.builder("ticketing_auth_attempts_total")
                .description("Total authentication attempts")
                .tag("service", "auth-guard")
                .register(meterRegistry);
    }

    @Bean
    public Counter authSuccessCounter() {
        return Counter.builder("ticketing_auth_success_total")
                .description("Successful JWT authentications and risk score passes")
                .tag("service", "auth-guard")
                .register(meterRegistry);
    }

    @Bean
    public Counter securityMacroDetectedCounter() {
        return Counter.builder("ticketing_security_macro_detected_total")
                .description("Total bot or macro detection events")
                .tag("service", "auth-guard")
                .register(meterRegistry);
    }

    @Bean
    public Counter securityBlockedIpCounter() {
        return Counter.builder("ticketing_security_blocked_ip_total")
                .description("Total IP blocking events")
                .tag("service", "auth-guard")
                .register(meterRegistry);
    }
}