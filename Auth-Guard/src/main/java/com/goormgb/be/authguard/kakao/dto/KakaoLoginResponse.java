package com.goormgb.be.authguard.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.enums.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "카카오 로그인 응답")
@Getter
@Builder
public class KakaoLoginResponse {

	@Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
	private String accessToken;
	@JsonIgnore
	private String refreshToken;
	@JsonIgnore
	private boolean newUser;
	@Schema(description = "유저 기본 정보")
	private UserInfo user;
	@Schema(description = "온보딩 진행 필요 여부", example = "true")
	private boolean onboardingRequired;

	@Schema(description = "유저 정보")
	@Getter
	@Builder
	public static class UserInfo {
		@Schema(description = "유저 ID", example = "1")
		private Long userId;
		@Schema(description = "이메일", example = "user@example.com")
		private String email;
		@Schema(description = "닉네임", example = "홍길동")
		private String nickname;
		@Schema(description = "프로필 이미지 URL", example = "https://k.kakaocdn.net/example.jpg")
		private String profileImageUrl;
		@Schema(description = "계정 상태", example = "ACTIVATE")
		private UserStatus status;
	}

	public static KakaoLoginResponse of(
			String accessToken,
			String refreshToken,
			User user,
			boolean newUser
	) {
		return KakaoLoginResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.newUser(newUser)
				.user(UserInfo.builder()
						.userId(user.getId())
						.email(user.getEmail())
						.nickname(user.getNickname())
						.profileImageUrl(user.getProfileImageUrl())
						.status(user.getStatus())
						.build())
				.onboardingRequired(!user.getOnboardingCompleted())
				.build();
	}
}
