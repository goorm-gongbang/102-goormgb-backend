package com.goormgb.be.authguard.jwt.provider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.goormgb.be.authguard.jwt.config.JwtProperties;
import com.goormgb.be.authguard.jwt.enums.TokenType;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: JwtTokenProvider 구현
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private static final String CLAIM_TOKEN_TYPE = "tokenType";
	private static final String CLAIM_AUTH = "auth";

	private final JwtProperties jwtProperties;
	private SecretKey secretKey;

	@PostConstruct
	public void init() {
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
	}

	public String createAccessToken(Long userId, String authority) {
		Instant now = Instant.now();
		Instant expiration = now.plus(jwtProperties.getAccessToken().getExpirationMinutes(), ChronoUnit.MINUTES);

		return Jwts.builder()
				.header()
				.type("JWT")
				.and()
				.issuer(jwtProperties.getIssuer())
				.subject(String.valueOf(userId))
				.audience()
				.add(jwtProperties.getAccessToken().getAudience())
				.and()
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiration))
				.id(UUID.randomUUID().toString())
				.claim(CLAIM_TOKEN_TYPE, TokenType.ACCESS.getValue())
				.claim(CLAIM_AUTH, authority)
				.signWith(secretKey)
				.compact();
	}

	public String createRefreshToken(Long userId) {
		Instant now = Instant.now();
		Instant expiration = now.plus(jwtProperties.getRefreshToken().getExpirationDays(), ChronoUnit.DAYS);

		return Jwts.builder()
				.header()
				.type("JWT")
				.and()
				.issuer(jwtProperties.getIssuer())
				.subject(String.valueOf(userId))
				.audience()
				.add(jwtProperties.getRefreshToken().getAudience())
				.and()
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiration))
				.id(UUID.randomUUID().toString())
				.claim(CLAIM_TOKEN_TYPE, TokenType.REFRESH.getValue())
				.signWith(secretKey)
				.compact();
	}

	public boolean validateToken(String token) {
		try {
			parseClaimsFromToken(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("Expired JWT token: {}", e.getMessage());
			throw new CustomException(ErrorCode.EXPIRED_TOKEN);
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}

	public Long getUserIdFromToken(String token) {
		Claims claims = parseClaimsFromToken(token);
		return Long.parseLong(claims.getSubject());
	}

	public String getAuthorityFromToken(String token) {
		Claims claims = parseClaimsFromToken(token);
		return claims.get(CLAIM_AUTH, String.class);
	}

	public TokenType getTokenTypeFromToken(String token) {
		Claims claims = parseClaimsFromToken(token);
		String tokenTypeValue = claims.get(CLAIM_TOKEN_TYPE, String.class);
		return TokenType.valueOf(tokenTypeValue);
	}

	public String getJtiFromToken(String token) {
		Claims claims = parseClaimsFromToken(token);
		return claims.getId();
	}

	public Date getExpirationFromToken(String token) {
		Claims claims = parseClaimsFromToken(token);
		return claims.getExpiration();
	}

	private Claims parseClaimsFromToken(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	/**
	 * 만료된 토큰도 허용하여 Claims를 반환한다.
	 * 서명이 유효하지 않거나 토큰 형식이 잘못된 경우에는 예외를 발생시킨다.
	 *
	 * @param token JWT 토큰
	 * @return Claims
	 */
	public Claims parseClaimsAllowExpired(String token) {
		try {
			return parseClaimsFromToken(token);
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}
}