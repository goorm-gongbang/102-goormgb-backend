package com.goormgb.be.ordercore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.goormgb.be.global.security.filter.XUserIdAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final XUserIdAuthenticationFilter xUserIdAuthenticationFilter;

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
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/swagger-resources/**",
								"/v3/api-docs/**",
								"/actuator/health/**",
								"/actuator/prometheus",
								"/clubs/**",
								"/matches/**"
						).permitAll()
						.anyRequest().authenticated()
				)
				.addFilterBefore(xUserIdAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
