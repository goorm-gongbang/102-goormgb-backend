package com.goormgb.be.global.encryption;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev", "test"})
public class NoOpEncryptionProvider implements EncryptionProvider {

	@Override
	public String encrypt(String plainText) {
		return plainText;
	}

	@Override
	public String decrypt(String cipherText) {
		return cipherText;
	}
}
