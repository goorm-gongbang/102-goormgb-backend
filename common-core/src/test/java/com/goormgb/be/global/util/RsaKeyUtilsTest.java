package com.goormgb.be.global.util;

import static org.assertj.core.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RsaKeyUtilsTest {

	private static KeyPair keyPair;
	private static String privateKeyBase64;
	private static String publicKeyBase64;

	@BeforeAll
	static void generateKeyPair() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		keyPair = kpg.generateKeyPair();
		privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
		publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
	}

	@Nested
	@DisplayName("parsePrivateKey")
	class ParsePrivateKey {

		@Test
		@DisplayName("base64 DER 문자열에서 RSAPrivateKey를 파싱한다")
		void parsesFromBase64Der() {
			RSAPrivateKey key = RsaKeyUtils.parsePrivateKey(privateKeyBase64);

			assertThat(key).isNotNull();
			assertThat(key.getAlgorithm()).isEqualTo("RSA");
		}

		@Test
		@DisplayName("파싱된 개인키는 원본 개인키와 같다")
		void parsedKeyEqualsOriginal() {
			RSAPrivateKey key = RsaKeyUtils.parsePrivateKey(privateKeyBase64);

			assertThat(key.getEncoded()).isEqualTo(keyPair.getPrivate().getEncoded());
		}

		@Test
		@DisplayName("PEM 헤더/푸터가 있는 문자열도 파싱된다")
		void parsesWithPemHeaders() {
			String withHeaders = "-----BEGIN PRIVATE KEY-----\n"
				+ privateKeyBase64 + "\n"
				+ "-----END PRIVATE KEY-----";

			RSAPrivateKey key = RsaKeyUtils.parsePrivateKey(withHeaders);

			assertThat(key).isNotNull();
			assertThat(key.getEncoded()).isEqualTo(keyPair.getPrivate().getEncoded());
		}

		@Test
		@DisplayName("개행문자가 포함된 base64 문자열도 파싱된다")
		void parsesWithWhitespace() {
			String withNewlines = privateKeyBase64.replaceAll("(.{64})", "$1\n");

			RSAPrivateKey key = RsaKeyUtils.parsePrivateKey(withNewlines);

			assertThat(key).isNotNull();
		}

		@Test
		@DisplayName("잘못된 형식의 문자열이면 IllegalStateException을 던진다")
		void invalidInput_throwsIllegalStateException() {
			// Base64로는 유효하지만 RSA 키 스펙에 맞지 않는 데이터
			String invalidKey = Base64.getEncoder().encodeToString("invalid-rsa-key-data".getBytes());
			assertThatThrownBy(() -> RsaKeyUtils.parsePrivateKey(invalidKey))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("RSA 개인키 로드 실패");
		}
	}

	@Nested
	@DisplayName("parsePublicKey")
	class ParsePublicKey {

		@Test
		@DisplayName("base64 DER 문자열에서 RSAPublicKey를 파싱한다")
		void parsesFromBase64Der() {
			RSAPublicKey key = RsaKeyUtils.parsePublicKey(publicKeyBase64);

			assertThat(key).isNotNull();
			assertThat(key.getAlgorithm()).isEqualTo("RSA");
		}

		@Test
		@DisplayName("파싱된 공개키는 원본 공개키와 같다")
		void parsedKeyEqualsOriginal() {
			RSAPublicKey key = RsaKeyUtils.parsePublicKey(publicKeyBase64);

			assertThat(key.getEncoded()).isEqualTo(keyPair.getPublic().getEncoded());
		}

		@Test
		@DisplayName("PEM 헤더/푸터가 있는 문자열도 파싱된다")
		void parsesWithPemHeaders() {
			String withHeaders = "-----BEGIN PUBLIC KEY-----\n"
				+ publicKeyBase64 + "\n"
				+ "-----END PUBLIC KEY-----";

			RSAPublicKey key = RsaKeyUtils.parsePublicKey(withHeaders);

			assertThat(key).isNotNull();
			assertThat(key.getEncoded()).isEqualTo(keyPair.getPublic().getEncoded());
		}

		@Test
		@DisplayName("개행문자가 포함된 base64 문자열도 파싱된다")
		void parsesWithWhitespace() {
			String withNewlines = publicKeyBase64.replaceAll("(.{64})", "$1\n");

			RSAPublicKey key = RsaKeyUtils.parsePublicKey(withNewlines);

			assertThat(key).isNotNull();
		}

		@Test
		@DisplayName("잘못된 형식의 문자열이면 IllegalStateException을 던진다")
		void invalidInput_throwsIllegalStateException() {
			// Base64로는 유효하지만 RSA 키 스펙에 맞지 않는 데이터
			String invalidKey = Base64.getEncoder().encodeToString("invalid-rsa-key-data".getBytes());
			assertThatThrownBy(() -> RsaKeyUtils.parsePublicKey(invalidKey))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("RSA 공개키 로드 실패");
		}
	}
}
