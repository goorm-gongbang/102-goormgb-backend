package com.goormgb.be.apigateway.jwt.util;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * API-Gateway 전용 RSA 공개키 로드 유틸리티.
 * <p>
 * API-Gateway는 WebFlux 기반으로 common-core(WebMVC) 의존을 추가할 수 없으므로
 * 공개키 로드 로직만 별도로 유지한다.
 * 검증(verify)만 수행 — 서명(sign) 불가.
 */
public final class RsaKeyUtils {

	private RsaKeyUtils() {
	}

	/**
	 * base64 DER 형식의 X.509 공개키 문자열을 RSAPublicKey로 변환한다.
	 *
	 * @param base64Der PEM 헤더·개행 없이 base64만 남긴 X.509 공개키 문자열
	 * @return RSAPublicKey
	 */
	public static RSAPublicKey parsePublicKey(String base64Der) {
		try {
			byte[] decoded = Base64.getDecoder().decode(sanitize(base64Der));
			X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return (RSAPublicKey)kf.generatePublic(spec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new IllegalStateException("RSA 공개키 로드 실패", e);
		}
	}

	private static String sanitize(String pem) {
		return pem
			.replaceAll("-----BEGIN[^-]*-----", "")
			.replaceAll("-----END[^-]*-----", "")
			.replaceAll("\\s", "");
	}
}
