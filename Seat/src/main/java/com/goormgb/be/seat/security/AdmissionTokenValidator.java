package com.goormgb.be.seat.security;

import java.security.interfaces.RSAPublicKey;

import org.springframework.stereotype.Component;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.global.util.RsaKeyUtils;
import com.goormgb.be.seat.config.AdmissionJwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdmissionTokenValidator {

	private static final String TOKEN_TYPE = "ADMISSION";
	private static final String CLAIM_MATCH_ID = "matchId";
	private static final String CLAIM_TYPE = "type";

	private final AdmissionJwtProperties admissionJwtProperties;
	private RSAPublicKey publicKey;

	@PostConstruct
	public void init() {
		this.publicKey = RsaKeyUtils.parsePublicKey(admissionJwtProperties.publicKey());
	}

	public void validate(String token, Long expectedUserId, Long expectedMatchId) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(publicKey)
				.requireIssuer(admissionJwtProperties.issuer())
				.build()
				.parseSignedClaims(token)
				.getPayload();

			Long userId = Long.valueOf(claims.getSubject());
			Long matchId = claims.get(CLAIM_MATCH_ID, Long.class);
			String type = claims.get(CLAIM_TYPE, String.class);

			Preconditions.validate(TOKEN_TYPE.equals(type), ErrorCode.INVALID_TOKEN);
			Preconditions.validate(expectedUserId.equals(userId), ErrorCode.INVALID_TOKEN);
			Preconditions.validate(expectedMatchId.equals(matchId), ErrorCode.INVALID_TOKEN);
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}
}
