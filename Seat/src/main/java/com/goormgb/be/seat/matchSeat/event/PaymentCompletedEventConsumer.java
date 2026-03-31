package com.goormgb.be.seat.matchSeat.event;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.PaymentCompletedEvent;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventConsumer {

	private final MatchSeatRepository matchSeatRepository;

	@KafkaListener(topics = EventTopic.PAYMENT_COMPLETED, groupId = "seat-service")
	@Transactional
	public void handlePaymentCompleted(PaymentCompletedEvent event) {
		List<Long> requestedIds = event.getMatchSeatIds();
		List<MatchSeat> seats = matchSeatRepository.findAllById(requestedIds);

		if (seats.size() != requestedIds.size()) {
			log.warn("[Kafka] 좌석 조회 수 불일치: orderId={}, 요청={}건, 조회={}건",
				event.getOrderId(), requestedIds.size(), seats.size());
		}

		int soldCount = 0;
		for (MatchSeat seat : seats) {
			if (seat.getSaleStatus() == MatchSeatSaleStatus.BLOCKED) {
				seat.markSold();
				soldCount++;
			} else if (seat.getSaleStatus() == MatchSeatSaleStatus.SOLD) {
				log.debug("[Kafka] 이미 SOLD 상태, 스킵: matchSeatId={}", seat.getId());
			} else {
				log.warn("[Kafka] 예상하지 못한 좌석 상태: matchSeatId={}, status={}, orderId={} — "
						+ "Consumer Lag으로 인해 SeatHoldCleanupScheduler가 먼저 AVAILABLE로 복원했을 가능성 있음",
					seat.getId(), seat.getSaleStatus(), event.getOrderId());
			}
		}

		if (soldCount > 0) {
			log.info("[Kafka] 결제 이벤트 처리 완료: orderId={}, 좌석 SOLD 전환={}건",
				event.getOrderId(), soldCount);
		} else {
			log.info("[Kafka] 결제 이벤트 수신: orderId={}, SOLD 전환 대상 없음",
				event.getOrderId());
		}
	}
}
