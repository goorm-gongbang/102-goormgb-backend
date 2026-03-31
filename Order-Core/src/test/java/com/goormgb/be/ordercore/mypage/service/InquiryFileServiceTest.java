package com.goormgb.be.ordercore.mypage.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.inquiry.entity.Inquiry;
import com.goormgb.be.ordercore.inquiry.enums.InquiryCategory;
import com.goormgb.be.ordercore.inquiry.repository.InquiryRepository;
import com.goormgb.be.ordercore.mypage.dto.response.InquiryFilePresignedResponse;
import com.goormgb.be.user.entity.User;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryFileService 단위 테스트")
class InquiryFileServiceTest {

	@Mock
	private InquiryRepository inquiryRepository;
	@Mock
	private S3Presigner s3Presigner;
	@Mock
	private S3Client s3Client;
	@Mock
	private PresignedPutObjectRequest presignedPutObjectRequest;
	@Mock
	private PresignedGetObjectRequest presignedGetObjectRequest;
	@Mock
	private ResponseInputStream<GetObjectResponse> responseInputStream;

	private InquiryFileService inquiryFileService;

	@BeforeEach
	void setUp() {
		inquiryFileService = new InquiryFileService(inquiryRepository, s3Presigner, s3Client);
		ReflectionTestUtils.setField(inquiryFileService, "bucket", "goormgb-qna");
		ReflectionTestUtils.setField(inquiryFileService, "prefix", "dev/");
	}

	@Nested
	@DisplayName("generatePresignedUrl")
	class GeneratePresignedUrl {

		@Test
		@DisplayName("본인 문의 + 허용 확장자면 presignedUrl/fileKey를 반환한다")
		void generatePresignedUrl_성공() throws Exception {
			Inquiry inquiry = createInquiry(11L, 1L);
			given(inquiryRepository.findById(11L)).willReturn(Optional.of(inquiry));
			given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(
				presignedPutObjectRequest);
			given(presignedPutObjectRequest.url()).willReturn(new URL("https://signed.example.com/put"));

			InquiryFilePresignedResponse response = inquiryFileService.generatePresignedUrl(1L, 11L, "seat.jpg");

			assertThat(response.presignedUrl()).isEqualTo("https://signed.example.com/put");
			assertThat(response.fileKey()).startsWith("dev/11/1/").endsWith(".jpg");
		}

		@Test
		@DisplayName("허용되지 않은 확장자면 예외가 발생한다")
		void generatePresignedUrl_확장자오류_예외() {
			Inquiry inquiry = createInquiry(11L, 1L);
			given(inquiryRepository.findById(11L)).willReturn(Optional.of(inquiry));

			assertThatThrownBy(() -> inquiryFileService.generatePresignedUrl(1L, 11L, "virus.exe"))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_FILE_TYPE_NOT_ALLOWED));
		}

		@Test
		@DisplayName("타인 문의면 예외가 발생한다")
		void generatePresignedUrl_권한없음_예외() {
			Inquiry inquiry = createInquiry(11L, 2L);
			given(inquiryRepository.findById(11L)).willReturn(Optional.of(inquiry));

			assertThatThrownBy(() -> inquiryFileService.generatePresignedUrl(1L, 11L, "seat.jpg"))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_ACCESS_DENIED));
		}
	}

	@Nested
	@DisplayName("confirmFile")
	class ConfirmFile {

		@Test
		@DisplayName("검증 통과 시 fileKey를 문의에 저장한다")
		void confirmFile_성공() throws Exception {
			Inquiry inquiry = createInquiry(11L, 1L);
			given(inquiryRepository.findById(11L)).willReturn(Optional.of(inquiry));
			given(s3Client.headObject(any(HeadObjectRequest.class)))
				.willReturn(HeadObjectResponse.builder().contentLength(1024L).build());
			given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(responseInputStream);
			given(responseInputStream.readNBytes(8))
				.willReturn(new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x00});

			inquiryFileService.confirmFile(1L, 11L, "dev/11/1/file.jpg");

			assertThat(inquiry.getFileKey()).isEqualTo("dev/11/1/file.jpg");
		}

		@Test
		@DisplayName("prefix가 다르면 예외가 발생하고 S3 조회를 수행하지 않는다")
		void confirmFile_prefix오류_예외() {
			Inquiry inquiry = createInquiry(11L, 1L);
			given(inquiryRepository.findById(11L)).willReturn(Optional.of(inquiry));

			assertThatThrownBy(() -> inquiryFileService.confirmFile(1L, 11L, "dev/11/2/file.jpg"))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_FILE_KEY_INVALID));

			then(s3Client).should(never()).headObject(any(HeadObjectRequest.class));
		}

		@Test
		@DisplayName("파일 크기 초과면 예외가 발생한다")
		void confirmFile_크기초과_예외() {
			Inquiry inquiry = createInquiry(11L, 1L);
			given(inquiryRepository.findById(11L)).willReturn(Optional.of(inquiry));
			given(s3Client.headObject(any(HeadObjectRequest.class)))
				.willReturn(HeadObjectResponse.builder().contentLength(5L * 1024 * 1024 + 1).build());

			assertThatThrownBy(() -> inquiryFileService.confirmFile(1L, 11L, "dev/11/1/file.jpg"))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_FILE_TOO_LARGE));
		}

		@Test
		@DisplayName("시그니처 불일치면 예외가 발생하고 S3 파일 삭제를 시도한다")
		void confirmFile_시그니처불일치_예외() throws Exception {
			Inquiry inquiry = createInquiry(11L, 1L);
			given(inquiryRepository.findById(11L)).willReturn(Optional.of(inquiry));
			given(s3Client.headObject(any(HeadObjectRequest.class)))
				.willReturn(HeadObjectResponse.builder().contentLength(1024L).build());
			given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(responseInputStream);
			given(responseInputStream.readNBytes(8))
				.willReturn(new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, 0x00, 0x00, 0x00});

			assertThatThrownBy(() -> inquiryFileService.confirmFile(1L, 11L, "dev/11/1/file.jpg"))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_FILE_SIGNATURE_MISMATCH));

			then(s3Client).should().deleteObject(any(DeleteObjectRequest.class));
		}

		@Test
		@DisplayName("S3 파일이 없으면 예외가 발생한다")
		void confirmFile_파일없음_예외() {
			Inquiry inquiry = createInquiry(11L, 1L);
			given(inquiryRepository.findById(11L)).willReturn(Optional.of(inquiry));
			given(s3Client.headObject(any(HeadObjectRequest.class)))
				.willThrow((S3Exception)S3Exception.builder().statusCode(404).build());

			assertThatThrownBy(() -> inquiryFileService.confirmFile(1L, 11L, "dev/11/1/file.jpg"))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_FILE_NOT_FOUND));
		}
	}

	@Test
	@DisplayName("generateDownloadUrl은 presigned GET URL을 반환한다")
	void generateDownloadUrl_성공() throws Exception {
		given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(
			presignedGetObjectRequest);
		given(presignedGetObjectRequest.url()).willReturn(new URL("https://signed.example.com/get"));

		String downloadUrl = inquiryFileService.generateDownloadUrl("dev/11/1/file.jpg");

		assertThat(downloadUrl).isEqualTo("https://signed.example.com/get");
	}

	private Inquiry createInquiry(Long inquiryId, Long userId) {
		User user = OrderFixture.createUserWithId(userId);
		Inquiry inquiry = Inquiry.create(
			user,
			InquiryCategory.BOOKING,
			"문의 제목",
			"문의 내용",
			"010-1234-5678"
		);
		ReflectionTestUtils.setField(inquiry, "id", inquiryId);
		return inquiry;
	}
}
