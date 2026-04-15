package com.goormgb.be.seat.matchSeat.event;

import java.time.Instant;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.OrderCancelledEvent;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventConsumer {

	private final MatchSeatRepository matchSeatRepository;
	private final MatchRepository matchRepository;

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

		updateMatchToOnSaleIfAnyAvailable(event.getMatchId(), event.getOrderId());
	}

	private void updateMatchToOnSaleIfAnyAvailable(Long matchId, Long orderId) {
		int updated = matchRepository.updateOnSaleIfAnyAvailableSeat(
			matchId,
			Instant.now(),
			SaleStatus.SOLD_OUT.name(),
			SaleStatus.ON_SALE.name(),
			MatchSeatSaleStatus.AVAILABLE.name()
		);
		if (updated > 0) {
			log.info("[Kafka] 경기 상태 복귀 - orderId={}, matchId={}, action=update, from=SOLD_OUT, to=ON_SALE",
				orderId, matchId);
			return;
		}

		log.debug("[Kafka] 경기 상태 복귀 스킵 - orderId={}, matchId={}, action=skip, reason=no_available_or_not_sold_out_or_started",
			orderId, matchId);
	}
}
