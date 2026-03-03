package com.goormgb.be.ordercore.club.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClubDetailFlatDto {

	private Long clubId;
	private String koName;
	private String logoImg;
	private String clubColor;

	private Long stadiumId;
	private String stadiumKoName;

	private String homepageRedirectUrl;

	private Integer seasonYear;
	private Integer seasonRanking;
	private Integer wins;
	private Integer draws;
	private Integer losses;
	private BigDecimal winRate;
	private BigDecimal battingAverage;
	private BigDecimal era;
	private BigDecimal gamesBehind;
}
