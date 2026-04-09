package com.goormgb.be.seat.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class PreQueueRedisConfig {

	@Bean(name = "preQueueRedisConnectionFactory")
	public LettuceConnectionFactory preQueueRedisConnectionFactory(
		@Value("${prequeue.redis.host:${REDIS_HOST}}") String host,
		@Value("${prequeue.redis.port:${REDIS_PORT}}") int port
	) {
		return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
	}

	@Bean(name = "preQueueStringRedisTemplate")
	public StringRedisTemplate preQueueStringRedisTemplate(
		@Qualifier("preQueueRedisConnectionFactory") LettuceConnectionFactory preQueueRedisConnectionFactory
	) {
		return new StringRedisTemplate(preQueueRedisConnectionFactory);
	}
}
