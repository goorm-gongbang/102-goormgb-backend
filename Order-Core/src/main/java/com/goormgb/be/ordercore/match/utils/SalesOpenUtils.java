package com.goormgb.be.ordercore.match.utils;

import com.goormgb.be.ordercore.match.entity.Match;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SalesOpenUtils {
    private static final int SALES_OPEN_DAYS_BEFORE_MATCH = 7;
    private static final int SALES_OPEN_HOUR = 11;
    private static final int SALES_OPEN_MINUTE = 0;

    public LocalDateTime calculateSalesOpenAt(Match match) {
        return match.getMatchAt()
                .minusDays(SALES_OPEN_DAYS_BEFORE_MATCH)
                .withHour(SALES_OPEN_HOUR)
                .withMinute(SALES_OPEN_MINUTE)
                .withSecond(0)
                .withNano(0);
    }
}
