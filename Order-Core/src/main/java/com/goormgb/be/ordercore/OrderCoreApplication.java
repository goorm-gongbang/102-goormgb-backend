package com.goormgb.be.ordercore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.goormgb.be")
@ConfigurationPropertiesScan(basePackages = "com.goormgb.be")
public class OrderCoreApplication {
	public static void main(String[] args) {
		SpringApplication.run(OrderCoreApplication.class, args);
	}
}
