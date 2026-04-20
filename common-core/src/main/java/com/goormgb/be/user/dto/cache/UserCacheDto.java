package com.goormgb.be.user.dto.cache;

import java.io.Serializable;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.enums.UserStatus;

/**
 * Redis 분산 캐시에 저장되는 사용자 스냅샷 DTO.
 *
 * <p>JPA 엔티티를 그대로 직렬화하는 대신 필요한 필드만 담은 불변 record 를 캐시에 저장한다.
 * JPA 프록시/Lazy 연관 직렬화 이슈를 피하고, 캐시 포맷이 엔티티 스키마 변경과 독립적으로 유지된다.</p>
 *
 * <p>포함하는 필드는 Auth-Guard 의 {@code /auth/me} / 토큰 재발급, Order-Core 의
 * 주문 생성·마이페이지 프로필 조회, Seat 의 추천 흐름에서 공통으로 필요로 하는 최소 집합이다.
 * 추가 필드가 필요해지면 이 DTO 와 {@link #from(User)} 변환부를 함께 확장한다.</p>
 *
 * <p>직렬화: {@code GenericJackson2JsonRedisSerializer} 기반 JSON. {@link Serializable} 를
 * 함께 구현해 비상 시 Java 직렬화 경로도 허용한다.</p>
 */
public record UserCacheDto(
	Long id,
	String email,
	String nickname,
	String profileImageUrl,
	UserStatus status,
	Boolean onboardingCompleted,
	Boolean marketingConsent,
	Instant lastLoginAt,
	Instant createdAt
) implements Serializable {

	@JsonCreator
	public UserCacheDto(
		@JsonProperty("id") Long id,
		@JsonProperty("email") String email,
		@JsonProperty("nickname") String nickname,
		@JsonProperty("profileImageUrl") String profileImageUrl,
		@JsonProperty("status") UserStatus status,
		@JsonProperty("onboardingCompleted") Boolean onboardingCompleted,
		@JsonProperty("marketingConsent") Boolean marketingConsent,
		@JsonProperty("lastLoginAt") Instant lastLoginAt,
		@JsonProperty("createdAt") Instant createdAt
	) {
		this.id = id;
		this.email = email;
		this.nickname = nickname;
		this.profileImageUrl = profileImageUrl;
		this.status = status;
		this.onboardingCompleted = onboardingCompleted;
		this.marketingConsent = marketingConsent;
		this.lastLoginAt = lastLoginAt;
		this.createdAt = createdAt;
	}

	/**
	 * 영속 엔티티로부터 스냅샷 DTO 를 생성한다. 호출 시점의 트랜잭션 내에서 필드가 초기화돼 있어야 한다.
	 */
	public static UserCacheDto from(User user) {
		return new UserCacheDto(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			user.getProfileImageUrl(),
			user.getStatus(),
			user.getOnboardingCompleted(),
			user.getMarketingConsent(),
			user.getLastLoginAt(),
			user.getCreatedAt()
		);
	}
}
