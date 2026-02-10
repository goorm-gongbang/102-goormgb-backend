package com.goormgb.be.user.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.user.enums.SocialProvider;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_sns", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"provider", "provider_user_id"})
}, indexes = {
		@Index(name = "idx_user_sns_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSns extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 20)
	private SocialProvider provider;

	@Column(name = "provider_user_id", nullable = false, length = 128)
	private String providerUserId;

	@Builder
	public UserSns(User user, SocialProvider provider, String providerUserId) {
		this.user = user;
		this.provider = provider != null ? provider : SocialProvider.KAKAO;
		this.providerUserId = providerUserId;
	}

	public static UserSns create(User user, SocialProvider provider, String providerUserId) {
		return UserSns.builder()
				.user(user)
				.provider(provider)
				.providerUserId(providerUserId)
				.build();
	}

}
