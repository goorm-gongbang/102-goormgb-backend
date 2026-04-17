package com.goormgb.be.kafka.event;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좌석 홀드(BLOCKED) 성공 시점에 발행되는 이벤트.
 *
 * <p>Queue 모듈이 이 이벤트를 수신해 해당 사용자의 READY 슬롯을 즉시 회수한다.
 * 결과적으로 스케줄러 주기가 돌아올 때 뒤에서 대기하던 사용자가 더 빠르게 승급된다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldCompletedEvent {

    private Long userId;
    private Long matchId;
    private List<Long> matchSeatIds;
    private Instant occurredAt;
}
