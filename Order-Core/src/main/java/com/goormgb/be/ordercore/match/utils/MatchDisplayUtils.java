package com.goormgb.be.ordercore.match.utils;

import com.goormgb.be.ordercore.match.dto.MatchGuideDto;
import com.goormgb.be.ordercore.match.entity.Match;
import com.goormgb.be.ordercore.match.enums.PurchaseStatus;
import com.goormgb.be.ordercore.match.enums.SaleStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Component
public class MatchDisplayUtils {

    public MatchGuideDto toGuide(Match match) {
        return MatchGuideDto.of(
                createTeamsDisplay(match),
                createAgeLimit(),
                createPlaceDisplay(match),
                createAddressDisplay(match),
                createDateTimeDisplay(match),
                createPurchaseStatus(match),
                createMatchDdayLabel(match)
        );
    }

    public String createTeamsDisplay(Match match){
        return match.getHomeClub().getKoName() + " vs " + match.getAwayClub().getKoName();
    }

    public String createAgeLimit(){
        return "전체관람가";
    }

    public String createPlaceDisplay(Match match) {
        return match.getStadium().getKoName();
    }

    public String createAddressDisplay(Match match){
        return match.getStadium().getAddress();
    }

    public String createDateTimeDisplay(Match match){
        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern("yyyy년 MM월 dd일", Locale.KOREAN);

        DateTimeFormatter timeFormatter =
                DateTimeFormatter.ofPattern("HH:mm");

        String dayOfWeek = match.getMatchAt().getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.KOREAN);

        return match.getMatchAt().format(dateFormatter)
                + " (" + dayOfWeek + ") "
                + match.getMatchAt().format(timeFormatter);
    }

    public PurchaseStatus createPurchaseStatus(Match match){
        return match.getSaleStatus() == SaleStatus.ON_SALE
                ? PurchaseStatus.PURCHASABLE
                : PurchaseStatus.NOT_PURCHASABLE;
    }

    public String createMatchDdayLabel(Match match){
        LocalDate today = LocalDate.now();
        LocalDate matchDate = match.getMatchAt().toLocalDate();

        long diff = ChronoUnit.DAYS.between(today, matchDate);

        if (diff > 0) return "D-" + diff;
        if (diff == 0) return "D-DAY";

        return "D+" + Math.abs(diff);
    }
}
