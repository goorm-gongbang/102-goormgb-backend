package com.goormgb.be.global.encryption;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * AES-GCM 암호화 컨버터.
 *
 * AES-GCM은 랜덤 IV를 사용하므로 동일 평문이라도 매번 다른 암호문이 생성된다.
 * Hibernate dirty checking 시 convertToDatabaseColumn()이 호출되면
 * 기존 DB 스냅샷과 다른 값이 반환되어 불필요한 UPDATE가 발생한다.
 *
 * 이를 방지하기 위해 ThreadLocal에 "DB에서 읽은 원본 암호문"을 캐싱하고,
 * 평문이 변경되지 않았으면 원본 암호문을 그대로 반환한다.
 */
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

	private static EncryptionProvider encryptionProvider;

	/**
	 * ThreadLocal 캐시: 복호화 시 (암호문 → 평문) 매핑을 저장.
	 * dirty checking 시 평문이 동일하면 원본 암호문을 재사용하여 불필요한 UPDATE 방지.
	 */
	private static final ThreadLocal<java.util.Map<String, String>> PLAINTEXT_TO_CIPHER_CACHE =
			ThreadLocal.withInitial(java.util.HashMap::new);

	@Slf4j
	@Component
	static class EncryptionProviderInjector implements ApplicationContextAware {
		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			try {
				EncryptionConverter.encryptionProvider = applicationContext.getBean(EncryptionProvider.class);
			} catch (NoSuchBeanDefinitionException e) {
				log.warn("[EncryptionConverter] EncryptionProvider 빈 없음 - NoOp 모드로 동작");
				EncryptionConverter.encryptionProvider = new NoOpEncryptionProvider();
			}
		}
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		if (attribute == null) {
			return null;
		}
		if (encryptionProvider == null) {
			throw new IllegalStateException("EncryptionProvider가 초기화되지 않았습니다.");
		}

		// 캐시에 동일 평문에 대한 원본 암호문이 있으면 재사용 (dirty checking 우회)
		String cached = PLAINTEXT_TO_CIPHER_CACHE.get().get(attribute);
		if (cached != null) {
			return cached;
		}

		return encryptionProvider.encrypt(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		if (encryptionProvider == null) {
			throw new IllegalStateException("EncryptionProvider가 초기화되지 않았습니다.");
		}

		String plainText = encryptionProvider.decrypt(dbData);

		// 복호화 시 (평문 → 원본 암호문) 캐싱
		PLAINTEXT_TO_CIPHER_CACHE.get().put(plainText, dbData);

		return plainText;
	}

	/**
	 * 요청 종료 시 ThreadLocal 캐시 정리.
	 * Filter/Interceptor에서 호출하거나, @Transactional 종료 후 자동 정리 용도.
	 */
	public static void clearCache() {
		PLAINTEXT_TO_CIPHER_CACHE.remove();
	}
}
