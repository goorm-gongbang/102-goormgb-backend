package com.goormgb.be.ordercore.state;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.club.entity.Club;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "team_season_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"club_id", "season_year"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamSeasonStats extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "season_year", nullable = false)
    private int seasonYear;

    @Column(name = "season_ranking")
    private int seasonRanking;

    private int wins;
    private int draws;
    private int losses;

    @Column(name = "win_rate", precision = 5, scale = 3)
    private BigDecimal winRate;

    @Column(name = "batting_average", precision = 5, scale = 3)
    private BigDecimal battingAverage;

    @Column(precision = 4, scale = 2)
    private BigDecimal era;

    @Column(name = "games_behind", precision = 4, scale = 1)
    private BigDecimal gamesBehind;

}
