package com.goormgb.be.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.goormgb.be")
@ConfigurationPropertiesScan(basePackages = "com.goormgb.be")
public class QueueCoreApplication {
	public static void main(String[] args) {
		SpringApplication.run(QueueCoreApplication.class, args);
	}

}