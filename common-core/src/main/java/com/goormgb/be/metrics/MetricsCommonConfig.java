package com.goormgb.be.metrics;

import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class MetricsCommonConfig {

	@Bean
	public MeterRegistryCustomizer<MeterRegistry> commonTags(Environment environment) {
		return registry -> registry.config().commonTags(
				"service", environment.getProperty("spring.application.name", "unknown"),
				"env", environment.getProperty("spring.profiles.active", "local")
		);
	}
}

