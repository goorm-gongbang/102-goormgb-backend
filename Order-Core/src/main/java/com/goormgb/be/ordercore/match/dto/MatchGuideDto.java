package com.goormgb.be.ordercore.match.dto;

import com.goormgb.be.ordercore.match.enums.PurchaseStatus;

public record MatchGuideDto(
        String teamsDisplay,
        String ageLimit,
        String placeDisplay,
        String addressDisplay,
        String datetimeDisplay,
        PurchaseStatus purchaseStatus,
        String matchDdayLabel
) {
    public static MatchGuideDto of(
            String teamsDisplay,
            String ageLimit,
            String placeDisplay,
            String addressDisplay,
            String datetimeDisplay,
            PurchaseStatus purchaseStatus,
            String matchDdayLabel)
    {
        return new MatchGuideDto(
                teamsDisplay,
                ageLimit,
                placeDisplay,
                addressDisplay,
                datetimeDisplay,
                purchaseStatus,
                matchDdayLabel
        );
    }
}