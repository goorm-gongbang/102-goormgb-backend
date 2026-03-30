package com.goormgb.be.global.encryption;

public interface EncryptionProvider {
	String encrypt(String plainText);

	String decrypt(String cipherText);
}
