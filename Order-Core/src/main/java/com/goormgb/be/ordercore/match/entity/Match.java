package com.goormgb.be.ordercore.match.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.match.enums.SaleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match extends BaseEntity {

    @Column(name = "match_at", nullable = false)
    private LocalDateTime matchAt;

    // TODO: club_id 연결
    @Column(name = "away_club_id", nullable = false)
    private Long awayClubId;

    // TODO: stadium_id 연결
    @Column(name = "stadium_id", nullable = false)
    private Long stadiumId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false, length = 20)
    private SaleStatus saleStatus;

    @Builder
    public Match(LocalDateTime matchAt, Long awayClubId, Long stadiumId, SaleStatus saleStatus) {
        this.matchAt = matchAt;
        this.awayClubId = awayClubId;
        this.stadiumId = stadiumId;
        this.saleStatus = saleStatus;
    }

    public static Match create(LocalDateTime matchAt, Long awayClubId, Long stadiumId, SaleStatus saleStatus) {
        return Match.builder()
                .matchAt(matchAt)
                .awayClubId(awayClubId)
                .stadiumId(stadiumId)
                .saleStatus(saleStatus)
                .build();
    }
}
