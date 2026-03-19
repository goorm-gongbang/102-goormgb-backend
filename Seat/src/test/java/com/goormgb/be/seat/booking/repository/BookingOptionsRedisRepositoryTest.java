package com.goormgb.be.seat.booking.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.seat.booking.model.BookingOptions;

class BookingOptionsRedisRepositoryTest {

	@Test
	@DisplayName("Redis에서 BookingOptions JSON을 읽어 변환한다")
	void 예매옵션_조회_성공() {
		// given
		String KEY_PREFIX = "seat:booking-options";
		long TTL_SECONDS = 900;

		StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);

		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		BookingOptionsRedisRepository repository = new BookingOptionsRedisRepository(
			redisTemplate, objectMapper, KEY_PREFIX, TTL_SECONDS);

		String json = """
			{
			  "userId": 7,
			  "matchId": 10,
			  "recommendationEnabled": false,
			  "ticketCount": 2,
			  "nearAdjacentToggle": true,
			  "createdAt": "2026-03-29T05:00:00Z"
			}
			""";

		given(valueOperations.get("seat:booking-options:10:7")).willReturn(json);

		// when
		BookingOptions options = repository.getByUserIdAndMatchIdOrThrow(7L, 10L);

		// then
		assertThat(options.userId()).isEqualTo(7L);
		assertThat(options.matchId()).isEqualTo(10L);
		assertThat(options.recommendationEnabled()).isFalse();
		assertThat(options.ticketCount()).isEqualTo(2);
		assertThat(options.nearAdjacentToggle()).isTrue();
	}

	@Test
	@DisplayName("Redis 값이 없으면 좌석 세션 없음 예외를 던진다")
	void 예매옵션_없음_예외() {
		// given
		String KEY_PREFIX = "seat:booking-options";
		long TTL_SECONDS = 900;

		StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);

		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		BookingOptionsRedisRepository repository = new BookingOptionsRedisRepository(
			redisTemplate, objectMapper, KEY_PREFIX, TTL_SECONDS);

		given(valueOperations.get("seat:booking-options:10:7")).willReturn(null);

		// when & then
		assertThatThrownBy(() -> repository.getByUserIdAndMatchIdOrThrow(7L, 10L))
			.isInstanceOf(CustomException.class);
	}
}
