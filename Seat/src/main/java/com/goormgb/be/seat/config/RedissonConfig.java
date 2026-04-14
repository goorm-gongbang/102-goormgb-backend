package com.goormgb.be.seat.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

	@Bean(destroyMethod = "shutdown")
	public RedissonClient redissonClient(DataRedisProperties redisProperties) {
		Config config = new Config();
		String scheme = redisProperties.getSsl().isEnabled() ? "rediss://" : "redis://";
		config.useSingleServer()
			.setAddress(scheme + redisProperties.getHost() + ":" + redisProperties.getPort());
		return Redisson.create(config);
	}
}
