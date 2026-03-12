package com.goormgb.be.apigateway.config;

import java.util.regex.Pattern;

import org.springframework.cloud.gateway.filter.headers.observation.DefaultGatewayObservationConvention;
import org.springframework.cloud.gateway.filter.headers.observation.GatewayContext;
import org.springframework.cloud.gateway.filter.headers.observation.GatewayObservationConvention;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.config.MeterFilter;

@Configuration
public class GatewayObservationConfig {

	private static final Pattern NUMERIC_SEGMENT = Pattern.compile("(?<=/)\\d+(?=/|$)");
	private static final Pattern UUID_SEGMENT = Pattern.compile(
			"(?<=/)[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");
	private static final String ROUTE_TAG = "route";
	private static final String UNKNOWN = "/unknown";

	@Bean
	GatewayObservationConvention routeTagGatewayObservationConvention() {
		return new DefaultGatewayObservationConvention() {

			@Override
			public KeyValues getLowCardinalityKeyValues(GatewayContext context) {
				KeyValues defaults = super.getLowCardinalityKeyValues(context);
				ServerHttpRequest request = context.getRequest();
				if (request == null) {
					return defaults.and(KeyValue.of(ROUTE_TAG, UNKNOWN));
				}
				String path = request.getURI().getPath();
				return defaults.and(KeyValue.of(ROUTE_TAG, normalizePath(path)));
			}
		};
	}

	@Bean
	MeterFilter gatewayRouteCardinalityLimiter() {
		return MeterFilter.maximumAllowableTags(
				"spring.cloud.gateway.requests", "route", 100,
				MeterFilter.deny()
		);
	}

	static String normalizePath(String path) {
		if (path == null || path.isBlank()) {
			return UNKNOWN;
		}
		String normalized = UUID_SEGMENT.matcher(path).replaceAll("{uuid}");
		normalized = NUMERIC_SEGMENT.matcher(normalized).replaceAll("{id}");
		return normalized;
	}
}