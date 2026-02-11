package com.goormgb.be.apidocs.controller;

import com.goormgb.be.apidocs.config.ApiDocsProperties;
import com.goormgb.be.apidocs.config.ApiDocsProperties.ServiceInfo;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class ApiDocsProxyController {

	private final ApiDocsProperties properties;
	private final RestClient restClient;

	public ApiDocsProxyController(ApiDocsProperties properties) {
		this.properties = properties;

		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(3));
		factory.setReadTimeout(Duration.ofSeconds(5));

		this.restClient = RestClient.builder()
				.requestFactory(factory)
				.build();
	}

	@GetMapping(value = "/proxy/v3/api-docs/{serviceKey}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String proxyApiDocs(@PathVariable String serviceKey) {
		ServiceInfo serviceInfo = properties.services().get(serviceKey);
		if (serviceInfo == null) {
			return unavailableSpec(serviceKey);
		}

		try {
			return restClient.get()
					.uri(serviceInfo.url() + "/v3/api-docs")
					.retrieve()
					.body(String.class);
		} catch (Exception e) {
			return unavailableSpec(serviceInfo.name());
		}
	}

	private String unavailableSpec(String serviceName) {
		return """
				{
				  "openapi": "3.1.0",
				  "info": {
				    "title": "%s (Unavailable)",
				    "description": "This service is currently unavailable.",
				    "version": "0.0.0"
				  },
				  "paths": {}
				}
				""".formatted(serviceName);
	}
}
