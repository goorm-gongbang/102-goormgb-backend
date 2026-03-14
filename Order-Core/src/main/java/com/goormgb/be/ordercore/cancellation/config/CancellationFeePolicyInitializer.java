package com.goormgb.be.ordercore.cancellation.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.ordercore.cancellation.entity.CancellationFeePolicy;
import com.goormgb.be.ordercore.cancellation.repository.CancellationFeePolicyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 취소 수수료 정책 시드 데이터.
 *
 * 정책 (불변):
 *   - 경기 당일 (D-0)        : 취소 불가
 *   - 경기 D-1 ~ D-6         : 티켓 금액의 10% + 예매 대행 수수료 환불 불가
 *   - 경기 D-7 이상           : 취소 수수료 없음. 예매 당일 취소 시에만 예매 대행 수수료 환불
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class CancellationFeePolicyInitializer implements CommandLineRunner {

	private final CancellationFeePolicyRepository cancellationFeePolicyRepository;

	@Override
	@Transactional
	public void run(String... args) {
		if (cancellationFeePolicyRepository.count() > 0) {
			log.info("[CancellationFeePolicyInitializer] 취소 수수료 정책 데이터 이미 존재 - 시드 스킵");
			return;
		}

		log.info("[CancellationFeePolicyInitializer] 취소 수수료 정책 시드 데이터 삽입 시작");

		cancellationFeePolicyRepository.saveAll(List.of(
			CancellationFeePolicy.builder()
				.daysBeforeMatchMin(0)
				.daysBeforeMatchMax(0)
				.cancellable(false)
				.ticketFeeRate(BigDecimal.ZERO)
				.bookingFeeRefundable(false)
				.build(),

			CancellationFeePolicy.builder()
				.daysBeforeMatchMin(1)
				.daysBeforeMatchMax(6)
				.cancellable(true)
				.ticketFeeRate(new BigDecimal("0.100"))
				.bookingFeeRefundable(false)
				.build(),

			CancellationFeePolicy.builder()
				.daysBeforeMatchMin(7)
				.daysBeforeMatchMax(null)
				.cancellable(true)
				.ticketFeeRate(BigDecimal.ZERO)
				.bookingFeeRefundable(true)
				.build()
		));

		log.info("[CancellationFeePolicyInitializer] 취소 수수료 정책 시드 데이터 삽입 완료");
	}
}
