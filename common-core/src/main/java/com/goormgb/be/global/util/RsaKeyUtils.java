package com.goormgb.be.global.util;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 키를 로드하는 유틸리티 클래스.
 * <p>
 * 환경변수 또는 application.yaml에서 주입된 base64 DER 문자열을
 * Java의 RSAPrivateKey / RSAPublicKey로 변환한다.
 * <p>
 * 저장 형식: PEM 헤더·푸터 및 개행 제거 후 base64 DER 문자열
 * - 개인키: PKCS#8 형식 (openssl pkcs8 -topk8 ... -nocrypt)
 * - 공개키: X.509 형식 (openssl rsa -pubout)
 */
public final class RsaKeyUtils {

	private RsaKeyUtils() {
	}

	/**
	 * base64 DER 형식의 PKCS#8 개인키 문자열을 RSAPrivateKey로 변환한다.
	 *
	 * @param base64Der PEM 헤더·개행 없이 base64만 남긴 PKCS#8 개인키 문자열
	 * @return RSAPrivateKey
	 */
	public static RSAPrivateKey parsePrivateKey(String base64Der) {
		try {
			byte[] decoded = Base64.getDecoder().decode(sanitize(base64Der));
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return (RSAPrivateKey)kf.generatePrivate(spec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new IllegalStateException("RSA 개인키 로드 실패", e);
		}
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

	/**
	 * PEM 헤더/푸터와 개행문자를 제거하여 순수 base64 DER 문자열로 정규화한다.
	 * 이미 헤더 없는 순수 base64 문자열이 입력되어도 정상 동작한다.
	 */
	private static String sanitize(String pem) {
		return pem
			.replaceAll("-----BEGIN[^-]*-----", "")
			.replaceAll("-----END[^-]*-----", "")
			.replaceAll("\\s", "");
	}
}
