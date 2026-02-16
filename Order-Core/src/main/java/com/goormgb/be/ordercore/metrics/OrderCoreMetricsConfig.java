package com.goormgb.be.ordercore.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderCoreMetricsConfig {

    @Bean
    public Counter paymentAttemptsCounter(MeterRegistry registry) {
        return Counter.builder("ticketing_payment_attempts_total")
                .description("Total payment attempts")
                .register(registry);
    }

    @Bean
    public Counter paymentSuccessCounter(MeterRegistry registry) {
        return Counter.builder("ticketing_payment_success_total")
                .description("Successful payment transactions")
                .register(registry);
    }

    @Bean
    public Counter paymentFailCounter(MeterRegistry registry) {
        return Counter.builder("ticketing_payment_fail_total")
                .description("Failed payment transactions")
                .register(registry);
    }

    @Bean
    public Timer paymentLatencyTimer(MeterRegistry registry) {
        return Timer.builder("ticketing_payment_latency_seconds")
                .description("Payment gateway response time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @Bean
    public Timer orderProcessTimer(MeterRegistry registry) {
        return Timer.builder("ticketing_order_process_seconds")
                .description("End-to-end order creation and payment latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }
}
