package com.goormgb.be.recommendation.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecommendationMetricsConfig {

    @Bean
    public Timer recommendProcessTimer(MeterRegistry registry) {
        return Timer.builder("ticketing_recommend_process_seconds")
                .description("Seat recommendation algorithm execution time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }
}
