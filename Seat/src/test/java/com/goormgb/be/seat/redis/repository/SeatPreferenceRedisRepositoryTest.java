package com.goormgb.be.seat.redis.repository;

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
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;
import com.goormgb.be.seat.redis.SeatSession;

class SeatPreferenceRedisRepositoryTest {

	@Test
	@DisplayName("Queue와 동일한 키 포맷(matchId:userId)과 JSON 포맷을 읽어 SeatSession으로 변환한다")
	void 큐_포맷_좌석선호도_조회_성공() {
		// given
		String KEY_PREFIX = "seat:preference";

		StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);

		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		SeatPreferenceRedisRepository repository = new SeatPreferenceRedisRepository(redisTemplate, objectMapper,
			KEY_PREFIX);

		String json = """
			{
			  "userId": 7,
			  "matchId": 10,
			  "recommendationEnabled": false,
			  "ticketCount": 2,
			  "preferredBlockIds": [220, 221],
			  "enteredAt": "2026-03-29T05:00:00Z"
			}
			""";

		given(valueOperations.get("seat:preference:10:7")).willReturn(json);

		// when
		SeatSession session = repository.getByUserIdAndMatchIdOrThrow(7L, 10L);

		// then
		assertThat(session.getUserId()).isEqualTo(7L);
		assertThat(session.getMatchId()).isEqualTo(10L);
		assertThat(session.isRecommendationEnabled()).isFalse();
		assertThat(session.getTicketCount()).isEqualTo(2);
		assertThat(session.getPreferredBlockIds()).containsExactly(220L, 221L);
	}

	@Test
	@DisplayName("Redis 값이 없으면 좌석 세션 없음 예외를 던진다")
	void 좌석세션_없음_예외() {
		// given
		String KEY_PREFIX = "seat:preference";

		StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);

		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		SeatPreferenceRedisRepository repository = new SeatPreferenceRedisRepository(redisTemplate, objectMapper,
			KEY_PREFIX);

		given(valueOperations.get("seat:preference:10:7")).willReturn(null);

		// when & then
		assertThatThrownBy(() -> repository.getByUserIdAndMatchIdOrThrow(7L, 10L))
			.isInstanceOf(CustomException.class);
	}
}