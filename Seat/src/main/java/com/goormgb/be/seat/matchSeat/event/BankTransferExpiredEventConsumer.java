package com.goormgb.be.seat.matchSeat.event;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.BankTransferExpiredEvent;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.matchSeat.repository.UserPurchasedSeatCountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankTransferExpiredEventConsumer {

	private final MatchSeatRepository matchSeatRepository;
	private final UserPurchasedSeatCountRepository userPurchasedSeatCountRepository;

	@KafkaListener(topics = EventTopic.BANK_TRANSFER_EXPIRED, groupId = "seat-service")
	@Transactional
	public void handleBankTransferExpired(BankTransferExpiredEvent event) {
		List<Long> requestedIds = event.getMatchSeatIds();
		List<MatchSeat> seats = matchSeatRepository.findAllById(requestedIds);
		int missingSeatCount = requestedIds.size() - seats.size();
		int updatedCount = 0;
		int alreadyTargetStateCount = 0;
		int unexpectedStateCount = 0;

		for (MatchSeat seat : seats) {
			if (seat.getSaleStatus() == MatchSeatSaleStatus.SOLD) {
				seat.markAvailable();
				updatedCount++;
				log.debug(
					"[Kafka] 무통장만료 좌석복원 - orderId={}, paymentId={}, matchSeatId={}, currentStatus={}, action=update, targetStatus=AVAILABLE",
					event.getOrderId(), event.getPaymentId(), seat.getId(), MatchSeatSaleStatus.SOLD);
			} else if (seat.getSaleStatus() == MatchSeatSaleStatus.AVAILABLE) {
				alreadyTargetStateCount++;
				log.debug(
					"[Kafka] 무통장만료 처리스킵 - orderId={}, paymentId={}, matchSeatId={}, currentStatus={}, action=skip, reason=already_available",
					event.getOrderId(), event.getPaymentId(), seat.getId(), seat.getSaleStatus());
			} else {
				unexpectedStateCount++;
				log.warn(
					"[Kafka] 무통장만료 비정상상태 - orderId={}, paymentId={}, matchSeatId={}, currentStatus={}, action=skip, reason=unexpected_state",
					event.getOrderId(), event.getPaymentId(), seat.getId(), seat.getSaleStatus());
			}
		}

		log.info(
			"[Kafka] 무통장 만료 이벤트 처리 요약: orderId={}, paymentId={}, requestedCount={}, updatedCount={}, alreadyTargetStateCount={}, unexpectedStateCount={}, missingSeatCount={}",
			event.getOrderId(),
			event.getPaymentId(),
			requestedIds.size(),
			updatedCount,
			alreadyTargetStateCount,
			unexpectedStateCount,
			missingSeatCount
		);

		// 구매 완료 좌석 수 Redis 카운터 갱신 (SOLD → AVAILABLE 복원된 수만큼 감소)
		// PaymentCompletedEvent 발행 시 카운터가 증가했으므로, 만료 시 반드시 감소해야 한다
		if (updatedCount > 0) {
			userPurchasedSeatCountRepository.decrement(event.getUserId(), event.getMatchId(), updatedCount);
		}
	}
}
