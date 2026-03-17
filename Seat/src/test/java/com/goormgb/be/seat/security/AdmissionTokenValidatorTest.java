package com.goormgb.be.seat.security;

import static org.assertj.core.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.config.AdmissionJwtProperties;

import io.jsonwebtoken.Jwts;

class AdmissionTokenValidatorTest {

	private static AdmissionTokenValidator validator;
	private static RSAPrivateKey privateKey;
	private static RSAPublicKey publicKey;
	private static final String ISSUER = "queue-service";

	@BeforeAll
	static void setUp() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keyPair = keyGen.generateKeyPair();
		privateKey = (RSAPrivateKey)keyPair.getPrivate();
		publicKey = (RSAPublicKey)keyPair.getPublic();

		String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
		AdmissionJwtProperties properties = new AdmissionJwtProperties(publicKeyBase64, ISSUER);

		validator = new AdmissionTokenValidator(properties);
		validator.init();
	}

	private String issueToken(Long userId, Long matchId, Duration ttl) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(String.valueOf(userId))
			.issuer(ISSUER)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(ttl)))
			.id(UUID.randomUUID().toString())
			.claim("matchId", matchId)
			.claim("type", "ADMISSION")
			.signWith(privateKey, Jwts.SIG.RS256)
			.compact();
	}

	@Test
	@DisplayName("유효한 어드미션 토큰 검증 성공")
	void 유효한_토큰_검증_성공() {
		String token = issueToken(1L, 100L, Duration.ofSeconds(30));

		assertThatCode(() -> validator.validate(token, 1L, 100L))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("만료된 토큰이면 INVALID_TOKEN 예외")
	void 만료된_토큰() {
		Instant past = Instant.now().minusSeconds(60);
		String token = Jwts.builder()
			.subject("1")
			.issuer(ISSUER)
			.issuedAt(Date.from(past.minusSeconds(30)))
			.expiration(Date.from(past))
			.id(UUID.randomUUID().toString())
			.claim("matchId", 100L)
			.claim("type", "ADMISSION")
			.signWith(privateKey, Jwts.SIG.RS256)
			.compact();

		assertThatThrownBy(() -> validator.validate(token, 1L, 100L))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException)e).getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
	}

	@Test
	@DisplayName("다른 키로 서명된 토큰이면 INVALID_TOKEN 예외")
	void 위조된_서명() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		RSAPrivateKey fakeKey = (RSAPrivateKey)keyGen.generateKeyPair().getPrivate();

		String token = Jwts.builder()
			.subject("1")
			.issuer(ISSUER)
			.issuedAt(Date.from(Instant.now()))
			.expiration(Date.from(Instant.now().plusSeconds(30)))
			.id(UUID.randomUUID().toString())
			.claim("matchId", 100L)
			.claim("type", "ADMISSION")
			.signWith(fakeKey, Jwts.SIG.RS256)
			.compact();

		assertThatThrownBy(() -> validator.validate(token, 1L, 100L))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException)e).getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
	}

	@Test
	@DisplayName("userId가 불일치하면 INVALID_TOKEN 예외")
	void userId_불일치() {
		String token = issueToken(1L, 100L, Duration.ofSeconds(30));

		assertThatThrownBy(() -> validator.validate(token, 999L, 100L))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException)e).getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
	}

	@Test
	@DisplayName("matchId가 불일치하면 INVALID_TOKEN 예외")
	void matchId_불일치() {
		String token = issueToken(1L, 100L, Duration.ofSeconds(30));

		assertThatThrownBy(() -> validator.validate(token, 1L, 999L))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException)e).getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
	}

	@Test
	@DisplayName("type이 ADMISSION이 아니면 INVALID_TOKEN 예외")
	void 잘못된_토큰_타입() {
		String token = Jwts.builder()
			.subject("1")
			.issuer(ISSUER)
			.issuedAt(Date.from(Instant.now()))
			.expiration(Date.from(Instant.now().plusSeconds(30)))
			.id(UUID.randomUUID().toString())
			.claim("matchId", 100L)
			.claim("type", "ACCESS")
			.signWith(privateKey, Jwts.SIG.RS256)
			.compact();

		assertThatThrownBy(() -> validator.validate(token, 1L, 100L))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException)e).getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
	}
}
