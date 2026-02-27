package com.goormgb.be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

	@Value("${server.servlet.context-path:}")
	private String contextPath;

	@Bean
	public OpenAPI customOpenAPI() {
		SecurityScheme securityScheme = new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.in(SecurityScheme.In.HEADER)
				.name("Authorization");

		SecurityRequirement securityRequirement = new SecurityRequirement()
				.addList("BearerAuth");

		return new OpenAPI()
				.info(new Info()
						.title("표고 API")
						.description("구름공방 백엔드 API 문서")
						.version("v1"))
				.addServersItem(new Server().url(contextPath.isEmpty() ? "/" : contextPath))
				.addSecurityItem(securityRequirement)
				.schemaRequirement("BearerAuth", securityScheme);
	}
}