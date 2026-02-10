package com.goormgb.be.authguard.auth.dto;

import java.time.Instant;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Redis에 저장되는 Refresh Token 정보를 담는 DTO.
 * <p>
 * Key: {@code refresh_token:{jti}}
 * <p>
 * TTL: 7일 (application.yaml의 jwt.refresh-token.expiration-days 참조)
 * <p>
 * 중복 로그인 허용: 유저당 여러 개의 Refresh Token 저장 가능
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefreshTokenInfo {

	/** 토큰 소유자 ID */
	private Long userId;

	/** Refresh Token JWT 문자열 */
	private String token;

	/** JWT ID - 토큰 고유 식별자 (UUID), Redis Key로 사용 */
	private String jti;

	/** 토큰 패밀리 ID - RTR(Refresh Token Rotation) 추적용 */
	private String tokenFamily;

	//    /** 토큰 발급 시각 */
	//    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
	//    private LocalDateTime issuedAt;
	//
	//    /** 토큰 만료 시각 */
	//    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
	//    private LocalDateTime expiresAt;

	/** 토큰 발급 시각 (UTC 기준 절대시간) */
	private Instant issuedAt;

	/** 토큰 만료 시각 (UTC 기준 절대시간) */
	private Instant expiresAt;

	/** 토큰 발급 시 User-Agent (브라우저/기기 정보) */
	private String userAgent;

	/** 토큰 발급 시 클라이언트 IP 주소 */
	private String ipAddress;
}