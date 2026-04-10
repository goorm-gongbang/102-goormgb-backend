package com.goormgb.be.kafka.event;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferExpiredEvent {

    private Long orderId;
    private Long userId;
    private Long matchId;
    private Long paymentId;
    private List<Long> matchSeatIds;
    private Instant occurredAt;
}
