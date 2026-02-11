package com.goormgb.be.apidocs.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api-docs")
public record ApiDocsProperties(Map<String, ServiceInfo> services) {

	public record ServiceInfo(String name, String url) {
	}
}
