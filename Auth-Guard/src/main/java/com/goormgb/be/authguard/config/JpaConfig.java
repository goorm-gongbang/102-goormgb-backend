package com.goormgb.be.authguard.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.goormgb.be")
@EntityScan(basePackages = "com.goormgb.be")
public class JpaConfig {
}
