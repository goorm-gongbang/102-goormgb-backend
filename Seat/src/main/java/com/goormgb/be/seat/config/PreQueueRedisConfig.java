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

	/**
	 * PreQueue 전용 StringRedisTemplate 설정
	 *
	 * <p>LettuceConnectionFactory를 @Bean으로 등록하지 않고 내부에서 직접 생성합니다.
	 * 이 방식은 Spring Boot의 RedisAutoConfiguration(기본 Redis 설정)을 방해하지 않으면서
	 * 특정 목적(PreQueue)을 위한 별도의 Redis 연결을 생성할 때 가장 안전한 방법입니다.</p>
	 */
	@Bean(name = "preQueueStringRedisTemplate")
	public StringRedisTemplate preQueueStringRedisTemplate() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);

		LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
			LettuceClientConfiguration.builder();

		if (sslEnabled) {
			clientConfigBuilder.useSsl();
		}

		LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfigBuilder.build());
		factory.afterPropertiesSet();

		return new StringRedisTemplate(factory);
	}
}