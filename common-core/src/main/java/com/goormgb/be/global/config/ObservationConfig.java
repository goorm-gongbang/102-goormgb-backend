package com.goormgb.be.global.config;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.ObservationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Observation configuration for adding custom metrics tags.
 * Adds X-Test-Id header value as 'test_id' label to HTTP server metrics.
 *
 * - Uses ObservationFilter for broader compatibility (MVC + WebFlux future support)
 * - Limits cardinality to prevent metric explosion in Prometheus
 */
@Configuration
public class ObservationConfig {

    /**
     * Adds test_id tag from X-Test-Id header to HTTP server metrics.
     * Works with Spring MVC ServerRequestObservationContext.
     */
    @Bean
    public ObservationFilter testIdObservationFilter() {
        return context -> {
            if (context instanceof ServerRequestObservationContext serverContext) {
                String testId = serverContext.getCarrier() != null
                    ? serverContext.getCarrier().getHeader("X-Test-Id")
                    : null;
                context.addLowCardinalityKeyValue(KeyValue.of("test_id", testId != null ? testId : "none"));
            }
            return context;
        };
    }

    /**
     * Limits test_id tag cardinality to prevent Prometheus index bloat.
     * Denies new metrics after 100 unique test_id values.
     */
    @Bean
    public MeterFilter testIdCardinalityFilter() {
        return MeterFilter.maximumAllowableTags(
            "http.server.requests",
            "test_id",
            100,
            MeterFilter.deny()
        );
    }
}
