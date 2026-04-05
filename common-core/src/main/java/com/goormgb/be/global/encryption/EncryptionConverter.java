package com.goormgb.be.global.encryption;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
 *
 * <h3>캐시 정리 전략</h3>
 * <ul>
 *   <li>웹 요청: {@link EncryptionCacheCleanupFilter}에서 요청 종료 시 정리</li>
 *   <li>비웹 컨텍스트(@Scheduled, @KafkaListener 등): {@link TransactionSynchronization}으로 트랜잭션 종료 시 자동 정리</li>
 * </ul>
 *
 * <h3>캐시 키 제약사항</h3>
 * 캐시 키는 평문(plaintext)이며 필드 구분 없이 저장된다.
 * {@link AttributeConverter}는 어떤 엔티티의 어떤 필드에서 호출되는지 알 수 없으므로,
 * 동일 엔티티 내에서 서로 다른 암호화 필드가 같은 평문 값을 가질 경우
 * 마지막 복호화된 암호문으로 덮어씌워질 수 있다.
 * 다만 email, nickname, profileImageUrl 등은 도메인 특성상 동일 값을 가질 확률이 극히 낮아
 * 실질적 영향은 없다.
 */
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

	private static EncryptionProvider encryptionProvider;

	private static final ThreadLocal<java.util.Map<String, String>> PLAINTEXT_TO_CIPHER_CACHE =
			ThreadLocal.withInitial(java.util.HashMap::new);

	/** 현재 트랜잭션에 정리 콜백이 이미 등록되었는지 추적 */
	private static final ThreadLocal<Boolean> CLEANUP_REGISTERED = ThreadLocal.withInitial(() -> Boolean.FALSE);

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

		// 비웹 컨텍스트(@Scheduled, @KafkaListener 등)에서도 트랜잭션 종료 시 캐시 정리
		registerTransactionCleanup();

		return plainText;
	}

	/**
	 * 트랜잭션 종료 시 ThreadLocal 캐시를 정리하는 콜백을 등록한다.
	 * 웹 요청은 EncryptionCacheCleanupFilter에서도 정리되므로 이중 안전장치 역할.
	 * 비웹 컨텍스트에서는 이 콜백이 유일한 정리 수단이다.
	 */
	private static void registerTransactionCleanup() {
		if (!CLEANUP_REGISTERED.get() && TransactionSynchronizationManager.isSynchronizationActive()) {
			CLEANUP_REGISTERED.set(Boolean.TRUE);
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					clearCache();
				}
			});
		}
	}

	/**
	 * ThreadLocal 캐시 정리.
	 * EncryptionCacheCleanupFilter(웹) 및 TransactionSynchronization(비웹)에서 호출된다.
	 */
	public static void clearCache() {
		PLAINTEXT_TO_CIPHER_CACHE.remove();
		CLEANUP_REGISTERED.remove();
	}
}
