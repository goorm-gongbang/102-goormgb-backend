package com.goormgb.be.domain.club.entity;

import com.goormgb.be.domain.statium.entity.Stadium;
import com.goormgb.be.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clubs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseEntity {

	@Column(name = "ko_name", nullable = false, length = 100)
	private String koName;

	@Column(name = "en_name", nullable = false, length = 100)
	private String enName;

	@Column(name = "logo_img", length = 255)
	private String logoImg;

	@Column(name = "club_color", length = 20)
	private String clubColor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stadium_id", nullable = false)
	private Stadium stadium;

	@Column(name = "homepage_redirect_url", length = 255)
	private String homepageRedirectUrl;

	@Builder
	public Club(String koName, String enName, String logoImg, String clubColor, Stadium stadium,
		String homepageRedirectUrl) {
		this.koName = koName;
		this.enName = enName;
		this.logoImg = logoImg;
		this.clubColor = clubColor;
		this.stadium = stadium;
		this.homepageRedirectUrl = homepageRedirectUrl;
	}
}
