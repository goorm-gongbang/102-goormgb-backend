package com.goormgb.be.apigateway.jwt.repository;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AccessTokenBlacklistRepositoryTest {

	@Mock
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@InjectMocks
	private AccessTokenBlacklistRepository blacklistRepository;

	@Test
	@DisplayName("블랙리스트에 존재하는 jti면 true를 반환한다")
	void isBlacklisted_whenKeyExists_returnsTrue() {
		String jti = "blacklisted-jti";
		when(reactiveRedisTemplate.hasKey("token_blacklist:" + jti))
				.thenReturn(Mono.just(true));

		StepVerifier.create(blacklistRepository.isBlacklisted(jti))
				.expectNext(true)
				.verifyComplete();
	}

	@Test
	@DisplayName("블랙리스트에 존재하지 않는 jti면 false를 반환한다")
	void isBlacklisted_whenKeyNotExists_returnsFalse() {
		String jti = "valid-jti";
		when(reactiveRedisTemplate.hasKey("token_blacklist:" + jti))
				.thenReturn(Mono.just(false));

		StepVerifier.create(blacklistRepository.isBlacklisted(jti))
				.expectNext(false)
				.verifyComplete();
	}
}