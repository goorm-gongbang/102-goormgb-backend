package com.goormgb.be.ordercore.match.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.club.entity.Club;
import com.goormgb.be.ordercore.match.enums.SaleStatus;
import com.goormgb.be.ordercore.stadium.entity.Stadium;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Entity
@Table(name = "matches", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"stadium_id", "match_at"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match extends BaseEntity {

    @Column(name = "match_at", nullable = false)
    private LocalDateTime matchAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_club_id", nullable = false)
    private Club homeClub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_club_id", nullable = false)
    private Club awayClub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false, length = 20)
    private SaleStatus saleStatus;

    @Builder
    public Match(LocalDateTime matchAt, Club homeClub, Club awayClub, Stadium stadium, SaleStatus saleStatus) {
        this.matchAt = matchAt;
        this.homeClub = homeClub;
        this.awayClub = awayClub;
        this.stadium = stadium;
        this.saleStatus = saleStatus;
    }

    public static Match create(LocalDateTime matchAt, Club homeClub, Club awayClub, Stadium stadium, SaleStatus saleStatus) {
        return Match.builder()
                .matchAt(matchAt)
                .homeClub(homeClub)
                .awayClub(awayClub)
                .stadium(stadium)
                .saleStatus(saleStatus)
                .build();
    }
}
