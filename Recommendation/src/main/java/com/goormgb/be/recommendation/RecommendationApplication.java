package com.goormgb.be.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.goormgb.be")
@ConfigurationPropertiesScan(basePackages = "com.goormgb.be")
public class RecommendationApplication {
	public static void main(String[] args) {
		SpringApplication.run(RecommendationApplication.class, args);
	}
}
