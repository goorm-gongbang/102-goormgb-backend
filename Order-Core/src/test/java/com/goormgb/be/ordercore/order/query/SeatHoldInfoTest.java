package com.goormgb.be.ordercore.order.query;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SeatHoldInfo 도메인 단위 테스트")
class SeatHoldInfoTest {

	private SeatHoldInfo createHoldInfo(Instant expiresAt) {
		return new SeatHoldInfo(1L, 101L, 1L, expiresAt, 1L, "블루석", 10L, "BLUE_01", 5, 12);
	}

	@Test
	@DisplayName("만료 시각이 현재보다 이전이면 isExpired()가 true를 반환한다")
	void isExpired_만료된_선점은_true() {
		SeatHoldInfo expired = createHoldInfo(Instant.now().minus(1, ChronoUnit.SECONDS));
		assertThat(expired.isExpired(Instant.now())).isTrue();
	}

	@Test
	@DisplayName("만료 시각이 현재보다 이후이면 isExpired()가 false를 반환한다")
	void isExpired_유효한_선점은_false() {
		SeatHoldInfo active = createHoldInfo(Instant.now().plus(10, ChronoUnit.MINUTES));
		assertThat(active.isExpired(Instant.now())).isFalse();
	}

	@Test
	@DisplayName("만료 시각이 훨씬 먼 미래이면 isExpired()가 false를 반환한다")
	void isExpired_먼_미래는_false() {
		SeatHoldInfo farFuture = createHoldInfo(Instant.now().plus(30, ChronoUnit.DAYS));
		assertThat(farFuture.isExpired(Instant.now())).isFalse();
	}

	@Test
	@DisplayName("1초 뒤에 만료되는 선점은 isExpired()가 false를 반환한다")
	void isExpired_1초_후_만료는_false() {
		SeatHoldInfo almostExpired = createHoldInfo(Instant.now().plus(1, ChronoUnit.SECONDS));
		assertThat(almostExpired.isExpired(Instant.now())).isFalse();
	}

	@Test
	@DisplayName("1초 전에 만료된 선점은 isExpired()가 true를 반환한다")
	void isExpired_1초_전_만료는_true() {
		SeatHoldInfo justExpired = createHoldInfo(Instant.now().minus(1, ChronoUnit.SECONDS));
		assertThat(justExpired.isExpired(Instant.now())).isTrue();
	}
}
