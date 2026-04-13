package com.goormgb.be.seat.matchSeat.event;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.OrderCancelledEvent;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.matchSeat.repository.UserPurchasedSeatCountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventConsumer {

	private final MatchSeatRepository matchSeatRepository;
	private final UserPurchasedSeatCountRepository userPurchasedSeatCountRepository;

	@KafkaListener(topics = EventTopic.ORDER_CANCELLED, groupId = "seat-service")
	@Transactional
	public void handleOrderCancelled(OrderCancelledEvent event) {
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
						"[Kafka] 주문취소 경기장 좌석복원 - orderId={}, matchSeatId={}, currentStatus={}, action=update, targetStatus=AVAILABLE",
						event.getOrderId(), seat.getId(), MatchSeatSaleStatus.SOLD);
			} else if (seat.getSaleStatus() == MatchSeatSaleStatus.AVAILABLE) {
				alreadyTargetStateCount++;
				log.debug(
						"[Kafka] 주문취소 처리스킵 - orderId={}, matchSeatId={}, currentStatus={}, action=skip, reason=already_available",
						event.getOrderId(), seat.getId(), seat.getSaleStatus());
			} else {
				unexpectedStateCount++;
				log.warn(
						"[Kafka] 주문취소 비정상상태 - orderId={}, matchSeatId={}, currentStatus={}, action=skip, reason=unexpected_state",
						event.getOrderId(), seat.getId(), seat.getSaleStatus());
			}
		}

		log.info(
				"[Kafka] 주문 취소 이벤트 처리 요약: orderId={}, requestedCount={}, updatedCount={}, alreadyTargetStateCount={}, unexpectedStateCount={}, missingSeatCount={}",
				event.getOrderId(),
				requestedIds.size(),
				updatedCount,
				alreadyTargetStateCount,
				unexpectedStateCount,
				missingSeatCount
		);

		// 구매 완료 좌석 수 Redis 카운터 갱신 (SOLD → AVAILABLE 복원된 수만큼 감소)
		if (updatedCount > 0) {
			userPurchasedSeatCountRepository.decrement(event.getUserId(), event.getMatchId(), updatedCount);
		}
	}
}
