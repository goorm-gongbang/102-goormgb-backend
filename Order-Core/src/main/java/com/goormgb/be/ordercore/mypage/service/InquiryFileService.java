package com.goormgb.be.ordercore.mypage.service;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.inquiry.entity.Inquiry;
import com.goormgb.be.ordercore.inquiry.repository.InquiryRepository;
import com.goormgb.be.ordercore.mypage.dto.response.InquiryFilePresignedResponse;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryFileService {

	private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");
	private static final Map<String, String> CONTENT_TYPES = Map.of(
		"jpg", "image/jpeg",
		"jpeg", "image/jpeg",
		"png", "image/png",
		"pdf", "application/pdf"
	);
	private static final Map<String, byte[]> SIGNATURES = Map.of(
		"jpg", new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF},
		"jpeg", new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF},
		"png", new byte[] {(byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A, (byte)0x1A,
			(byte)0x0A},
		"pdf", new byte[] {(byte)0x25, (byte)0x50, (byte)0x44, (byte)0x46, (byte)0x2D}
	);

	private final InquiryRepository inquiryRepository;
	private final S3Presigner s3Presigner;
	private final S3Client s3Client;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	@Value("${cloud.aws.s3.prefix}")
	private String prefix;

	public InquiryFilePresignedResponse generatePresignedUrl(Long userId, Long inquiryId, String fileName) {
		findOwnedInquiry(userId, inquiryId);

		String extension = extractExtension(fileName);
		Preconditions.validate(ALLOWED_EXTENSIONS.contains(extension), ErrorCode.INQUIRY_FILE_TYPE_NOT_ALLOWED);
		String contentType = CONTENT_TYPES.get(extension);
		String key = buildFileKey(inquiryId, userId, extension);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.contentType(contentType)
			.contentDisposition("attachment")
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(5))
			.putObjectRequest(putObjectRequest)
			.build();

		PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
		return InquiryFilePresignedResponse.of(presigned.url().toString(), key);
	}

	public String generateDownloadUrl(String fileKey) {
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucket)
			.key(fileKey)
			.responseContentDisposition("attachment")
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(30))
			.getObjectRequest(getObjectRequest)
			.build();

		return s3Presigner.presignGetObject(presignRequest).url().toString();
	}

	@Transactional
	public void confirmFile(Long userId, Long inquiryId, String fileKey) {
		Inquiry inquiry = findOwnedInquiry(userId, inquiryId);
		validateKeyPrefix(fileKey, inquiryId, userId);

		String extension = extractExtension(fileKey);
		validateObjectSize(fileKey);
		validateObjectSignature(fileKey, extension);

		inquiry.updateFileKey(fileKey);
	}

	private Inquiry findOwnedInquiry(Long userId, Long inquiryId) {
		Inquiry inquiry = inquiryRepository.findById(inquiryId)
			.orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
		Preconditions.validate(inquiry.getUser().getId().equals(userId), ErrorCode.INQUIRY_ACCESS_DENIED);
		return inquiry;
	}

	private String getNormalizedPrefix() {
		return prefix.endsWith("/") ? prefix : prefix + "/";
	}

	private String buildFileKey(Long inquiryId, Long userId, String extension) {
		return getNormalizedPrefix() + inquiryId + "/" + userId + "/" + UUID.randomUUID() + "." + extension;
	}

	private void validateKeyPrefix(String fileKey, Long inquiryId, Long userId) {
		if (fileKey == null || fileKey.isBlank()) {
			throw new CustomException(ErrorCode.INQUIRY_FILE_KEY_INVALID);
		}
		String expectedPrefix = getNormalizedPrefix() + inquiryId + "/" + userId + "/";
		Preconditions.validate(fileKey.startsWith(expectedPrefix), ErrorCode.INQUIRY_FILE_KEY_INVALID);
		Preconditions.validate(!fileKey.contains(".."), ErrorCode.INQUIRY_FILE_KEY_INVALID);
	}

	private void validateObjectSize(String fileKey) {
		try {
			HeadObjectResponse response = s3Client.headObject(
				HeadObjectRequest.builder()
					.bucket(bucket)
					.key(fileKey)
					.build()
			);
			Long fileSize = response.contentLength();
			Preconditions.validate(fileSize != null && fileSize <= MAX_FILE_SIZE_BYTES,
				ErrorCode.INQUIRY_FILE_TOO_LARGE);
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				throw new CustomException(ErrorCode.INQUIRY_FILE_NOT_FOUND, e);
			}
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
		}
	}

	private void validateObjectSignature(String fileKey, String extension) {
		byte[] expectedSignature = SIGNATURES.get(extension);
		if (expectedSignature == null) {
			throw new CustomException(ErrorCode.INQUIRY_FILE_TYPE_NOT_ALLOWED);
		}

		byte[] actualSignature;
		try (ResponseInputStream<?> input = s3Client.getObject(GetObjectRequest.builder()
			.bucket(bucket)
			.key(fileKey)
			.range("bytes=0-7")
			.build())) {
			actualSignature = input.readNBytes(8);
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				throw new CustomException(ErrorCode.INQUIRY_FILE_NOT_FOUND, e);
			}
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
		} catch (Exception e) {
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
		}

		if (!startsWith(actualSignature, expectedSignature)) {
			deleteObjectQuietly(fileKey);
			throw new CustomException(ErrorCode.INQUIRY_FILE_SIGNATURE_MISMATCH);
		}
	}

	private String extractExtension(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			throw new CustomException(ErrorCode.INQUIRY_FILE_TYPE_NOT_ALLOWED);
		}

		int dot = fileName.lastIndexOf('.');
		if (dot < 0 || dot == fileName.length() - 1) {
			throw new CustomException(ErrorCode.INQUIRY_FILE_TYPE_NOT_ALLOWED);
		}

		String extension = fileName.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
		Preconditions.validate(!extension.isEmpty(), ErrorCode.INQUIRY_FILE_TYPE_NOT_ALLOWED);
		return extension;
	}

	private boolean startsWith(byte[] actual, byte[] expected) {
		if (actual.length < expected.length) {
			return false;
		}
		for (int i = 0; i < expected.length; i++) {
			if (actual[i] != expected[i]) {
				return false;
			}
		}
		return true;
	}

	private void deleteObjectQuietly(String fileKey) {
		try {
			s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(bucket)
				.key(fileKey)
				.build());
		} catch (Exception ignored) {
			// cleanup best-effort
		}
	}
}
