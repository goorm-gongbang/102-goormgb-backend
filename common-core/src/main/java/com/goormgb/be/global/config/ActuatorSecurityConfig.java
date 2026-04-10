package com.goormgb.be.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator 엔드포인트용 SecurityFilterChain.
 * 프로필에 따라 인증 정책을 분리한다.
 * - local / dev / test: 모든 actuator 엔드포인트 허용 (개발 편의)
 * - staging / prod: health, prometheus만 허용, 나머지 차단
 */
@Configuration
public class ActuatorSecurityConfig {

	/**
	 * local / dev / test 환경: 모든 actuator 엔드포인트 비인증 허용
	 */
	@Bean
	@Order(0)
	@Profile({"local", "dev", "test"})
	public SecurityFilterChain actuatorSecurityFilterChainDev(HttpSecurity http) throws Exception {
		http.securityMatcher("/actuator/**")
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}

	/**
	 * staging / prod 환경: health, prometheus만 허용, 나머지 거부
	 */
	@Bean
	@Order(0)
	@Profile({"staging", "prod"})
	public SecurityFilterChain actuatorSecurityFilterChainProd(HttpSecurity http) throws Exception {
		http.securityMatcher("/actuator/**")
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
				.anyRequest().denyAll()
			)
			.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}
}
