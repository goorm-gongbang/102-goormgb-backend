package com.goormgb.be.authguard.metrics;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthMetricsService {

	private final Counter authAttemptsCounter;
	private final Counter authSuccessCounter;
	private final Counter securityMacroDetectedCounter;
	private final Counter securityBlockedIpCounter;

	public void increaseAuthAttempt() {
		authAttemptsCounter.increment();
	}

	public void increaseAuthSuccess() {
		authSuccessCounter.increment();
	}

	public void increaseMacroDetected() {
		securityMacroDetectedCounter.increment();
	}

	public void increaseBlockedIp() {
		securityBlockedIpCounter.increment();
	}
}
