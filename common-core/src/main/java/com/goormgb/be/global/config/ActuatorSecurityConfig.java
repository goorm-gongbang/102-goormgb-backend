package com.goormgb.be.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator 엔드포인트용 SecurityFilterChain (management 포트 분리 시 필요)
 * 메인 SecurityFilterChain의 커스텀 필터들이 actuator 경로에 적용되지 않도록 분리
 */
@Configuration
public class ActuatorSecurityConfig {

	@Bean
	@Order(0)
	public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/actuator/**")
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}
}
