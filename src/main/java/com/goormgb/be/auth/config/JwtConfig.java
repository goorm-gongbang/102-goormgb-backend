package com.goormgb.be.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
// JWT설정 클래스 생성
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
}
