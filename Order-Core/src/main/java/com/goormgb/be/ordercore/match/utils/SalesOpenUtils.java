package com.goormgb.be.ordercore.match.utils;

import com.goormgb.be.ordercore.match.entity.Match;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SalesOpenUtils {
    public LocalDateTime calculateSalesOpenAt(Match match) {
        return match.getMatchAt()
                .minusDays(7)
                .withHour(11)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }
}
