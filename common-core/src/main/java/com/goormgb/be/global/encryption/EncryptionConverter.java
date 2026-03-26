package com.goormgb.be.global.encryption;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

	private static EncryptionProvider encryptionProvider;

	@Component
	static class EncryptionProviderInjector implements ApplicationContextAware {
		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			EncryptionConverter.encryptionProvider = applicationContext.getBean(EncryptionProvider.class);
		}
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		if (attribute == null) {
			return null;
		}
		return encryptionProvider.encrypt(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		return encryptionProvider.decrypt(dbData);
	}
}
