package com.goormgb.be.authguard.metrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class AuthMetricsConfig {

	@Bean
	public Counter authAttemptsCounter(MeterRegistry registry) {
		return Counter.builder("ticketing_auth_attempts_total")
				.description("Total authentication attempts")
				.register(registry);
	}

	@Bean
	public Counter authSuccessCounter(MeterRegistry registry) {
		return Counter.builder("ticketing_auth_success_total")
				.description("Successful authentications")
				.register(registry);
	}

	@Bean
	public Counter securityMacroDetectedCounter(MeterRegistry registry) {
		return Counter.builder("ticketing_security_macro_detected_total")
				.description("Macro detected events")
				.register(registry);
	}

	@Bean
	public Counter securityBlockedIpCounter(MeterRegistry registry) {
		return Counter.builder("ticketing_security_blocked_ip_total")
				.description("Blocked IP events")
				.register(registry);
	}
}
