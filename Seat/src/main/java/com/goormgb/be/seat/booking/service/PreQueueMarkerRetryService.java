package com.goormgb.be.seat.booking.service;

import org.springframework.stereotype.Component;

import com.goormgb.be.seat.booking.repository.PreQueueBookingOptionMarkerRepository;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;

/**
 * prequeue booking-option 마커 동기화 전용 재시도 서비스 (Phase 4).
 *
 * <p>기존 {@code BookingOptionsService#syncPreQueueMarkerOrRollback} 는 실패 시
 * {@code Thread.sleep(50/100/150ms)} 로 최대 300ms 동안 Tomcat worker 스레드를 점유한 채 재시도했다.
 * 1000 VU 이상 부하에서는 이 블로킹 재시도가 Tomcat 스레드 풀을 빠르게 소진시키는 주요 원인이었다.</p>
 *
 * <p>본 클래스는 해당 재시도를 Resilience4j {@code @Retry} 로 위임한다. 대기 자체가 AOP 레이어에서
 * 처리되고, 실패 시 예외를 그대로 던져 상위 서비스가 롤백 트랜잭션을 수행할 수 있도록 한다.
 * 재시도 간격(30ms × 1.5 지수)을 짧게 가져가 스레드 점유 시간을 기존 300ms → 최대 ~100ms 수준으로 단축한다.</p>
 *
 * <p>Resilience4j 설정은 {@code application.yaml} 의 {@code resilience4j.retry.instances.prequeueMark} 블록을 참조한다.</p>
 */
@Component
@RequiredArgsConstructor
public class PreQueueMarkerRetryService {

	private final PreQueueBookingOptionMarkerRepository preQueueBookingOptionMarkerRepository;

	/**
	 * prequeue 마커를 기록한다. 실패 시 Resilience4j 설정(attempts, wait-duration)에 따라 자동 재시도한다.
	 *
	 * @throws RuntimeException 최대 재시도 횟수 초과 후 여전히 실패한 경우. 호출자가 보상 트랜잭션을 수행해야 한다.
	 */
	@Retry(name = "prequeueMark")
	public void mark(Long matchId, Long userId) {
		preQueueBookingOptionMarkerRepository.mark(matchId, userId);
	}
}
