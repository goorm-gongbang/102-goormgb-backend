package com.goormgb.be.ordercore.payment.scheduler;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.enums.PaymentStatus;
import com.goormgb.be.ordercore.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 무통장 입금 기한 만료 시 자동 취소 스케줄러.
 *
 * <p>입금 기한(depositDeadline)이 지났는데 여전히 PENDING 상태인 무통장 입금 건을 찾아
 * 주문을 자동 취소하고 좌석을 AVAILABLE로 복원한다.</p>
 *
 * <p>각 결제 취소는 {@link BankTransferCancelService}에서 개별 트랜잭션으로 처리되므로,
 * 한 건의 실패가 다른 건에 영향을 주지 않는다.</p>
 *
 * <h3>입금 기한 규칙</h3>
 * <ul>
 *   <li>일반: 다음날 23:59 KST</li>
 *   <li>경기 당일 예매: 경기 시작 3시간 전</li>
 *   <li>둘 중 빠른 시각 적용</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankTransferExpireScheduler {

	private final PaymentRepository paymentRepository;
	private final BankTransferCancelService bankTransferCancelService;

	/**
	 * 5분마다 만료된 무통장 입금 건을 조회하여 자동 취소한다.
	 */
	@Scheduled(fixedDelay = 300_000)
	public void cancelExpiredBankTransfers() {
		Instant now = Instant.now();
		List<Payment> expiredPayments = paymentRepository.findExpiredBankTransfers(PaymentStatus.PENDING, now);

		if (expiredPayments.isEmpty()) {
			return;
		}

		int cancelledCount = 0;
		int failedCount = 0;

		for (Payment payment : expiredPayments) {
			try {
				boolean cancelled = bankTransferCancelService.cancelSinglePayment(payment);
				if (cancelled) {
					cancelledCount++;
				}
			} catch (Exception e) {
				failedCount++;
				log.error("[BankTransferExpireScheduler] 자동 취소 실패 - paymentId={}: {}",
						payment.getId(), e.getMessage(), e);
			}
		}

		if (cancelledCount > 0) {
			log.info("[BankTransferExpireScheduler] 무통장 입금 기한 만료 자동 취소: {}건", cancelledCount);
		}
		if (failedCount > 0) {
			log.warn("[BankTransferExpireScheduler] 자동 취소 실패: {}건", failedCount);
		}
	}
}
