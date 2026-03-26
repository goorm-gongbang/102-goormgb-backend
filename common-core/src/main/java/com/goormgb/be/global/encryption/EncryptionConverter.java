package com.goormgb.be.global.encryption;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

	private static EncryptionProvider encryptionProvider;

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
		return encryptionProvider.decrypt(dbData);
	}
}
