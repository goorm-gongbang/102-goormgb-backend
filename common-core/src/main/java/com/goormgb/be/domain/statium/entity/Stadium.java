package com.goormgb.be.domain.statium.entity;

import com.goormgb.be.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stadiums")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stadium extends BaseEntity {

	@Column(name = "region", nullable = false, length = 50)
	private String region;

	@Column(name = "ko_name", nullable = false, length = 100)
	private String koName;

	@Column(name = "en_name", nullable = false, length = 100)
	private String enName;

	@Column(name = "address", length = 255)
	private String address;

	@Builder
	public Stadium(String region, String koName, String enName, String address) {
		this.region = region;
		this.koName = koName;
		this.enName = enName;
		this.address = address;
	}
}
