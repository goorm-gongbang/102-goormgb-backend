package com.goormgb.be.seat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admission.jwt")
public record AdmissionJwtProperties(
	String publicKey,
	String issuer
) {
}
