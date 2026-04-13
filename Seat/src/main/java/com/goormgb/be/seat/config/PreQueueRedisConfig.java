package com.goormgb.be.seat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class PreQueueRedisConfig {

	@Value("${prequeue.redis.host:${REDIS_QUEUE_HOST}}")
	private String host;

	@Value("${prequeue.redis.port:${REDIS_QUEUE_PORT}}")
	private int port;

	/**
	 * PreQueue 전용 StringRedisTemplate 설정
	 * * [참고]
	 * LettuceConnectionFactory를 @Bean으로 등록하지 않고 내부에서 직접 생성합니다.
	 * 이 방식은 Spring Boot의 RedisAutoConfiguration(기본 Redis 설정)을 방해하지 않으면서
	 * 특정 목적(PreQueue)을 위한 별도의 Redis 연결을 생성할 때 가장 안전한 방법입니다.
	 */
	@Bean(name = "preQueueStringRedisTemplate")
	public StringRedisTemplate preQueueStringRedisTemplate() {
		// 1. PreQueue 전용 Standalone 설정 생성
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);

		// 2. ConnectionFactory를 Bean이 아닌 일반 객체로 생성
		LettuceConnectionFactory factory = new LettuceConnectionFactory(config);

		// 3. (중요) Bean 관리 대상이 아니므로 수동으로 초기화 메서드를 호출해야 연결이 활성화됩니다.
		factory.afterPropertiesSet();

		// 4. 생성된 전용 Factory를 주입한 템플릿 반환
		return new StringRedisTemplate(factory);
	}
}