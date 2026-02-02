package com.goormgb.be.user.entity;

import com.goormgb.be.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dev_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DevUser extends BaseEntity {

	@Column(name = "login_id", nullable = false, unique = true, length = 50)
	private String loginId;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Builder
	public DevUser(String loginId, String passwordHash, User user) {
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.user = user;
	}
}