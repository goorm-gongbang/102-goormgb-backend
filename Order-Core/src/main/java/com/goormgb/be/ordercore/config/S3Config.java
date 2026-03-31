package com.goormgb.be.ordercore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

	@Bean
	public StaticCredentialsProvider credentialsProvider(
		@Value("${cloud.aws.credentials.access-key}") String accessKey,
		@Value("${cloud.aws.credentials.secret-key}") String secretKey
	) {
		return StaticCredentialsProvider.create(
			AwsBasicCredentials.create(accessKey, secretKey)
		);
	}

	@Bean
	public S3Presigner s3Presigner(
		@Value("${cloud.aws.s3.region}") String region,
		StaticCredentialsProvider credentialsProvider
	) {
		return S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(credentialsProvider)
			.build();
	}

	@Bean
	public S3Client s3Client(
		@Value("${cloud.aws.s3.region}") String region,
		StaticCredentialsProvider credentialsProvider
	) {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(credentialsProvider)
			.build();
	}
}
