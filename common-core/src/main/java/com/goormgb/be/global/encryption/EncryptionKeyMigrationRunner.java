package com.goormgb.be.global.encryption;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * DB 암호화 키 마이그레이션 Runner.
 *
 * 사용법:
 * 1. Secrets Manager에 OLD_DB_ENCRYPTION_KEY, DB_ENCRYPTION_KEY(신규) 설정
 * 2. ENCRYPTION_MIGRATION_ENABLED=true 환경변수 추가하여 앱 재시작
 * 3. 로그에서 마이그레이션 완료 확인 후 OLD_DB_ENCRYPTION_KEY, ENCRYPTION_MIGRATION_ENABLED 삭제
 */
@Slf4j
@Component
@Profile("staging")
@ConditionalOnProperty(name = "encryption.migration.enabled", havingValue = "true")
public class EncryptionKeyMigrationRunner implements ApplicationRunner {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;

	private final JdbcTemplate jdbcTemplate;
	private final SecretKey oldKey;
	private final SecretKey newKey;
	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * 마이그레이션 대상 테이블과 암호화 컬럼 매핑.
	 * key: 테이블명, value: 암호화된 컬럼 목록
	 */
	private static final Map<String, List<String>> TARGET_TABLES = Map.of(
			"users", List.of("email", "nickname", "profile_image_url"),
			"user_sns", List.of("provider_user_id"),
			"orders", List.of("orderer_name", "orderer_email", "orderer_phone", "orderer_birth_date"),
			"inquiries", List.of("phone_number"),
			"cash_receipts", List.of("number"),
			"payments", List.of("account_number", "account_holder")
	);

	public EncryptionKeyMigrationRunner(
			JdbcTemplate jdbcTemplate,
			@Value("${encryption.old-db-key}") String oldBase64Key,
			@Value("${encryption.db-key}") String newBase64Key) {
		this.jdbcTemplate = jdbcTemplate;
		this.oldKey = toSecretKey(oldBase64Key, "OLD_DB_ENCRYPTION_KEY");
		this.newKey = toSecretKey(newBase64Key, "DB_ENCRYPTION_KEY");
	}

	private SecretKey toSecretKey(String base64Key, String keyName) {
		byte[] keyBytes = Base64.getDecoder().decode(base64Key);
		if (keyBytes.length != 32) {
			throw new IllegalArgumentException(keyName + " must be 256 bits (32 bytes)");
		}
		return new SecretKeySpec(keyBytes, "AES");
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		log.info("===== DB 암호화 키 마이그레이션 시작 =====");

		int totalMigrated = 0;
		int totalSkipped = 0;

		for (Map.Entry<String, List<String>> entry : TARGET_TABLES.entrySet()) {
			String table = entry.getKey();
			List<String> columns = entry.getValue();

			log.info("[{}] 마이그레이션 시작 (컬럼: {})", table, columns);

			String selectColumns = "id, " + String.join(", ", columns);
			String sql = "SELECT " + selectColumns + " FROM " + table;

			List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
			int migrated = 0;
			int skipped = 0;

			for (Map<String, Object> row : rows) {
				Long id = ((Number)row.get("id")).longValue();

				StringBuilder updateSql = new StringBuilder("UPDATE " + table + " SET ");
				boolean hasUpdate = false;

				for (int i = 0; i < columns.size(); i++) {
					String column = columns.get(i);
					String cipherText = (String)row.get(column);

					if (cipherText == null) {
						continue;
					}

					String reEncrypted = reEncrypt(id, table, column, cipherText);
					if (reEncrypted == null) {
						continue;
					}

					if (hasUpdate) {
						updateSql.append(", ");
					}
					updateSql.append(column).append(" = '").append(reEncrypted).append("'");
					hasUpdate = true;
				}

				if (hasUpdate) {
					updateSql.append(" WHERE id = ").append(id);
					jdbcTemplate.update(updateSql.toString());
					migrated++;
				} else {
					skipped++;
				}
			}

			log.info("[{}] 완료 — 마이그레이션: {}건, 스킵(NULL): {}건", table, migrated, skipped);
			totalMigrated += migrated;
			totalSkipped += skipped;
		}

		log.info("===== DB 암호화 키 마이그레이션 완료 — 총 {}건 변환, {}건 스킵 =====", totalMigrated, totalSkipped);
	}

	/**
	 * 구 키로 복호화 → 신 키로 재암호화.
	 *
	 * @return 재암호화된 문자열, 또는 실패 시 null
	 */
	private String reEncrypt(Long id, String table, String column, String cipherText) {
		try {
			String plainText = decrypt(cipherText, oldKey);
			return encrypt(plainText, newKey);
		} catch (Exception e) {
			log.error("[{}.{}] id={} 마이그레이션 실패: {}", table, column, id, e.getMessage());
			throw new IllegalStateException(
					String.format("[%s.%s] id=%d 마이그레이션 실패 — 전체 롤백", table, column, id), e);
		}
	}

	private String encrypt(String plainText, SecretKey key) {
		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			secureRandom.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

			byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

			ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
			buffer.put(iv);
			buffer.put(cipherBytes);

			return Base64.getEncoder().encodeToString(buffer.array());
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("암호화 실패", e);
		}
	}

	private String decrypt(String cipherText, SecretKey key) {
		try {
			byte[] decoded = Base64.getDecoder().decode(cipherText);

			ByteBuffer buffer = ByteBuffer.wrap(decoded);
			byte[] iv = new byte[GCM_IV_LENGTH];
			buffer.get(iv);

			byte[] encrypted = new byte[buffer.remaining()];
			buffer.get(encrypted);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

			byte[] plainBytes = cipher.doFinal(encrypted);
			return new String(plainBytes, StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("복호화 실패", e);
		}
	}
}