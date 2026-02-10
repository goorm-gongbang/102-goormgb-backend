package com.goormgb.be.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.goormgb.be")
@ConfigurationPropertiesScan(basePackages = "com.goormgb.be")
@EnableJpaRepositories(basePackages = "com.goormgb.be")
@EntityScan(basePackages = "com.goormgb.be")
public class QueueApplication {
	public static void main(String[] args) {
		SpringApplication.run(QueueApplication.class, args);
	}

}