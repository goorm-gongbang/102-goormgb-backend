package com.goormgb.be.seat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class PreQueueRedisConfig {

	@Value("${prequeue.redis.host:${REDIS_QUEUE_HOST}}")
	private String host;

	@Value("${prequeue.redis.port:${REDIS_QUEUE_PORT}}")
	private int port;

	@Value("${spring.data.redis.ssl.enabled:false}")
	private boolean sslEnabled;

	@Bean(name = "preQueueStringRedisTemplate")
	public StringRedisTemplate preQueueStringRedisTemplate() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);

		LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
			LettuceClientConfiguration.builder();
		if (sslEnabled) {
			clientConfigBuilder.useSsl().disablePeerVerification();
		}

		LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfigBuilder.build());
		factory.afterPropertiesSet();

		return new StringRedisTemplate(factory);
	}
}