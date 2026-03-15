package com.goormgb.be.queue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue.security.jwt")
public record QueueJwtProperties(
	String privateKey,
	String publicKey,
	String issuer
) {
}
