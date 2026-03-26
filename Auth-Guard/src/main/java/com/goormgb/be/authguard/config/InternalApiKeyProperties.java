package com.goormgb.be.authguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "internal")
public class InternalApiKeyProperties {
	private final String apiKey;
}
