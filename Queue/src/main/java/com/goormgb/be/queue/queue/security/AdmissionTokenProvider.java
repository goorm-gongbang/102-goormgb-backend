package com.goormgb.be.queue.queue.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.global.util.RsaKeyUtils;
import com.goormgb.be.queue.config.QueueJwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;

@Component
public class AdmissionTokenProvider {

	private static final String TOKEN_TYPE = "ADMISSION";
	private static final String CLAIM_MATCH_ID = "matchId";
	private static final String CLAIM_TYPE = "type";

	private final QueueJwtProperties queueJwtProperties;
	private RSAPrivateKey privateKey;
	private RSAPublicKey publicKey;

	public AdmissionTokenProvider(QueueJwtProperties queueJwtProperties) {
		this.queueJwtProperties = queueJwtProperties;
	}

	@PostConstruct
	public void init() {
		this.privateKey = RsaKeyUtils.parsePrivateKey(queueJwtProperties.privateKey());
		this.publicKey = RsaKeyUtils.parsePublicKey(queueJwtProperties.publicKey());
	}

	// READY 승급 시 Queue가 발급하는 1회성 Seat 입장용 JWT다.
	public String issue(Long userId, Long matchId, Duration ttl) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(ttl);

		return Jwts.builder()
			.subject(String.valueOf(userId))
			.issuer(queueJwtProperties.issuer())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.id(UUID.randomUUID().toString())
			.claim(CLAIM_MATCH_ID, matchId)
			.claim(CLAIM_TYPE, TOKEN_TYPE)
			.signWith(privateKey, Jwts.SIG.RS256)
			.compact();
	}

	// 토큰 값 자체가 Redis READY 값과 일치하는지 확인하기 전에 서명/claim이 정상인지 1차 검증한다.
	public AdmissionTokenClaims validate(String token, Long expectedUserId, Long expectedMatchId) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(publicKey)
				.requireIssuer(queueJwtProperties.issuer())
				.build()
				.parseSignedClaims(token)
				.getPayload();

			Long userId = Long.valueOf(claims.getSubject());
			Long matchId = claims.get(CLAIM_MATCH_ID, Long.class);
			String type = claims.get(CLAIM_TYPE, String.class);

			Preconditions.validate(
				TOKEN_TYPE.equals(type) && expectedUserId.equals(userId) && expectedMatchId.equals(matchId),
				ErrorCode.INVALID_TOKEN);

			return new AdmissionTokenClaims(
				userId,
				matchId,
				claims.getId(),
				claims.getIssuedAt().toInstant(),
				claims.getExpiration().toInstant()
			);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}
}
