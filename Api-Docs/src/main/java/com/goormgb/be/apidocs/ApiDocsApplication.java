package com.goormgb.be.apidocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiDocsApplication {
	public static void main(String[] args) {
		SpringApplication.run(ApiDocsApplication.class, args);
	}
}
