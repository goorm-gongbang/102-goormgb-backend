package com.goormgb.be.global.config;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Observation configuration for adding custom metrics tags.
 * Adds X-Test-Id header value as 'test_id' label to HTTP server metrics.
 */
@Configuration
public class ObservationConfig {

    @Bean
    public DefaultServerRequestObservationConvention serverRequestObservationConvention() {
        return new DefaultServerRequestObservationConvention() {
            @Override
            public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
                return super.getLowCardinalityKeyValues(context)
                    .and(testIdTag(context));
            }

            private KeyValue testIdTag(ServerRequestObservationContext context) {
                String testId = context.getCarrier().getHeader("X-Test-Id");
                return KeyValue.of("test_id", testId != null ? testId : "none");
            }
        };
    }
}
