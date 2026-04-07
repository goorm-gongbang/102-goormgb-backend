package com.goormgb.be.authguard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormgb.be.authguard.filter.InternalApiKeyFilter;
import com.goormgb.be.authguard.jwt.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthGuardSecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final InternalApiKeyProperties internalApiKeyProperties;
	private final ObjectMapper objectMapper;

	/**
	 * Actuator 엔드포인트용 SecurityFilterChain (management 포트 분리 시 필요)
	 * 메인 SecurityFilterChain의 커스텀 필터들이 actuator 경로에 적용되지 않도록 분리
	 */
	@Bean
	@Order(0)
	public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/actuator/**")
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	@Order(1)
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.headers(headers -> headers
					.contentTypeOptions(ctOptions -> {})
					.frameOptions(frame -> frame.deny())
					.httpStrictTransportSecurity(hsts -> hsts
						.includeSubDomains(true)
						.maxAgeInSeconds(31536000))
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								// Internal API (API Key 필터로 보호)
								"/internal/users/{userId}/block",
								"/internal/users/{userId}/unblock",
								// Kakao OAuth
								"/kakao/login-url",
								"/kakao/login",
								// Token
								"/token/refresh",
								// Dev Auth (local/dev/test 프로필 전용)
								"/dev/auth/signup",
								"/dev/auth/login",
								"/dev/auth/test/500",
								// Load Test Auth
								"/loadtest/signup",
								"/loadtest/login",
								// Swagger & Actuator
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/swagger-resources/**",
								"/v3/api-docs/**",
								"/actuator/health/**",
								"/actuator/prometheus"
						).permitAll()
						.anyRequest().authenticated()
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(internalApiKeyFilter(), jwtAuthenticationFilter.getClass());

		return http.build();
	}

	@Bean
	public InternalApiKeyFilter internalApiKeyFilter() {
		return new InternalApiKeyFilter(internalApiKeyProperties, objectMapper);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
