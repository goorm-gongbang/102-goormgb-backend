package com.goormgb.be.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JvmMetricsRegistrar {

    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void registerVirtualThreadGauge() {
        Gauge.builder("jvm_threads_virtual_count", () ->
                        Thread.getAllStackTraces().keySet().stream()
                                .filter(Thread::isVirtual)
                                .count())
                .description("Current number of active virtual threads (Java 21)")
                .register(meterRegistry);
    }
}
