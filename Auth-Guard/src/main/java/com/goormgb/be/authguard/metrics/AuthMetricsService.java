package com.goormgb.be.authguard.metrics;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class AuthMetricsService {

	private static final String METRIC_AUTH_ATTEMPTS_TOTAL = "ticketing_auth_attempts_total";
	private static final String METRIC_AUTH_SUCCESS_TOTAL = "ticketing_auth_success_total";
	private static final String METRIC_SECURITY_MACRO_DETECTED_TOTAL = "ticketing_security_macro_detected_total";
	private static final String METRIC_SECURITY_BLOCKED_IP_TOTAL = "ticketing_security_blocked_ip_total";
	private static final String METRIC_AUTH_USER_BLOCKED_TOTAL = "ticketing_auth_user_blocked_total";
	private static final String METRIC_AUTH_USER_UNBLOCKED_TOTAL = "ticketing_auth_user_unblocked_total";

	private final MeterRegistry meterRegistry;

	public AuthMetricsService(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void increaseAuthAttempt() {
		meterRegistry.counter(METRIC_AUTH_ATTEMPTS_TOTAL).increment();
	}

	public void increaseAuthSuccess() {
		meterRegistry.counter(METRIC_AUTH_SUCCESS_TOTAL).increment();
	}

	public void increaseMacroDetected() {
		meterRegistry.counter(METRIC_SECURITY_MACRO_DETECTED_TOTAL).increment();
	}

	public void increaseBlockedIp() {
		meterRegistry.counter(METRIC_SECURITY_BLOCKED_IP_TOTAL).increment();
	}

	public void increaseUserBlocked() {
		meterRegistry.counter(METRIC_AUTH_USER_BLOCKED_TOTAL).increment();
	}

	public void increaseUserUnblocked() {
		meterRegistry.counter(METRIC_AUTH_USER_UNBLOCKED_TOTAL).increment();
	}
}
