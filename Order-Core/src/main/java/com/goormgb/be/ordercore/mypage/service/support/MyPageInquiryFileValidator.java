package com.goormgb.be.ordercore.mypage.service.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;

@Component
public class MyPageInquiryFileValidator {

	private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");

	private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES_BY_EXTENSION = Map.of(
		"jpg", Set.of("image/jpeg"),
		"jpeg", Set.of("image/jpeg"),
		"png", Set.of("image/png"),
		"pdf", Set.of("application/pdf")
	);

	public void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return;
		}

		Preconditions.validate(file.getSize() <= MAX_FILE_SIZE_BYTES, ErrorCode.INQUIRY_FILE_TOO_LARGE);

		String extension = extractExtension(file.getOriginalFilename());
		Preconditions.validate(ALLOWED_EXTENSIONS.contains(extension), ErrorCode.INQUIRY_FILE_TYPE_NOT_ALLOWED);

		String contentType = normalize(file.getContentType());
		Set<String> allowedContentTypes = ALLOWED_CONTENT_TYPES_BY_EXTENSION.get(extension);
		Preconditions.validate(
			allowedContentTypes != null && allowedContentTypes.contains(contentType),
			ErrorCode.INQUIRY_FILE_TYPE_NOT_ALLOWED
		);

		Preconditions.validate(matchesSignature(extension, file), ErrorCode.INQUIRY_FILE_SIGNATURE_MISMATCH);
	}

	private String extractExtension(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			return "";
		}

		int lastDot = originalFilename.lastIndexOf('.');
		if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
			return "";
		}
		return normalize(originalFilename.substring(lastDot + 1));
	}

	private String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private boolean matchesSignature(String extension, MultipartFile file) {
		try (InputStream inputStream = file.getInputStream()) {
			return switch (extension) {
				case "jpg", "jpeg" -> startsWith(inputStream, (byte)0xFF, (byte)0xD8, (byte)0xFF);
				case "png" -> startsWith(inputStream,
					(byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47,
					(byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A);
				case "pdf" -> startsWith(inputStream, (byte)0x25, (byte)0x50, (byte)0x44, (byte)0x46, (byte)0x2D);
				default -> false;
			};
		} catch (IOException e) {
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
		}
	}

	private boolean startsWith(InputStream inputStream, byte... signature) throws IOException {
		byte[] actual = inputStream.readNBytes(signature.length);
		if (actual.length != signature.length) {
			return false;
		}

		for (int i = 0; i < signature.length; i++) {
			if (actual[i] != signature[i]) {
				return false;
			}
		}
		return true;
	}
}
