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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankTransferExpiredEventConsumer {

	private final MatchSeatRepository matchSeatRepository;

	@KafkaListener(topics = EventTopic.BANK_TRANSFER_EXPIRED, groupId = "seat-service")
	@Transactional
	public void handleBankTransferExpired(BankTransferExpiredEvent event) {
		List<Long> requestedIds = event.getMatchSeatIds();
		List<MatchSeat> seats = matchSeatRepository.findAllById(requestedIds);

		if (seats.size() != requestedIds.size()) {
			log.warn("[Kafka] 좌석 조회 수 불일치: orderId={}, 요청={}건, 조회={}건",
				event.getOrderId(), requestedIds.size(), seats.size());
		}

		int restoredCount = 0;
		for (MatchSeat seat : seats) {
			if (seat.getSaleStatus() == MatchSeatSaleStatus.SOLD) {
				seat.markAvailable();
				restoredCount++;
			} else if (seat.getSaleStatus() == MatchSeatSaleStatus.AVAILABLE) {
				log.debug("[Kafka] 이미 AVAILABLE 상태, 스킵: matchSeatId={}", seat.getId());
			} else {
				log.warn("[Kafka] 예상하지 못한 좌석 상태: matchSeatId={}, status={}, orderId={}",
					seat.getId(), seat.getSaleStatus(), event.getOrderId());
			}
		}

		if (restoredCount > 0) {
			log.info("[Kafka] 무통장 만료 이벤트 처리 완료: orderId={}, paymentId={}, 좌석 AVAILABLE 복원={}건",
				event.getOrderId(), event.getPaymentId(), restoredCount);
		} else {
			log.info("[Kafka] 무통장 만료 이벤트 수신: orderId={}, AVAILABLE 복원 대상 없음",
				event.getOrderId());
		}
	}
}
