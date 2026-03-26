package com.goormgb.be.global.encryption;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({"staging", "prod"})
public class AesEncryptionProvider implements EncryptionProvider {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;

	private final SecureRandom secureRandom = new SecureRandom();

	@Value("${encryption.db-key}")
	private String base64Key;

	private SecretKey secretKey;

	@PostConstruct
	public void init() {
		byte[] keyBytes = Base64.getDecoder().decode(base64Key);
		if (keyBytes.length != 32) {
			throw new IllegalArgumentException("DB_ENCRYPTION_KEY must be 256 bits (32 bytes)");
		}
		this.secretKey = new SecretKeySpec(keyBytes, "AES");
		log.info("[AesEncryptionProvider] DB 암호화 활성화됨 (AES-256-GCM)");
	}

	@Override
	public String encrypt(String plainText) {
		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			secureRandom.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

			byte[] cipherText = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

			ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
			byteBuffer.put(iv);
			byteBuffer.put(cipherText);

			return Base64.getEncoder().encodeToString(byteBuffer.array());
		} catch (Exception e) {
			throw new IllegalStateException("DB 암호화 실패", e);
		}
	}

	@Override
	public String decrypt(String cipherText) {
		try {
			byte[] decoded = Base64.getDecoder().decode(cipherText);

			ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
			byte[] iv = new byte[GCM_IV_LENGTH];
			byteBuffer.get(iv);

			byte[] encrypted = new byte[byteBuffer.remaining()];
			byteBuffer.get(encrypted);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

			byte[] plainText = cipher.doFinal(encrypted);
			return new String(plainText, java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("DB 복호화 실패", e);
		}
	}
}
