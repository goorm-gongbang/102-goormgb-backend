package com.goormgb.be.ordercore.mypage.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.inquiry.entity.Inquiry;
import com.goormgb.be.ordercore.inquiry.repository.InquiryRepository;
import com.goormgb.be.ordercore.mypage.dto.request.MyPageInquiryCreateRequest;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryCreateResponse;
import com.goormgb.be.ordercore.mypage.service.support.MyPageInquiryFileValidator;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyPageInquiryService 서비스 단위 테스트")
class MyPageInquiryServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private InquiryRepository inquiryRepository;

	private MyPageInquiryService myPageInquiryService;

	@BeforeEach
	void setUp() {
		myPageInquiryService = new MyPageInquiryService(
			userRepository,
			inquiryRepository,
			new MyPageInquiryFileValidator()
		);
	}

	@Nested
	@DisplayName("createInquiry")
	class CreateInquiry {

		@Test
		@DisplayName("파일 없이 문의를 등록할 수 있다")
		void createInquiry_파일없음_성공() {
			User user = OrderFixture.createUser();
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(inquiryRepository.save(any(Inquiry.class))).willAnswer(invocation -> {
				Inquiry inquiry = invocation.getArgument(0);
				ReflectionTestUtils.setField(inquiry, "id", 11L);
				return inquiry;
			});

			MyPageInquiryCreateResponse response = myPageInquiryService.createInquiry(
				1L,
				new MyPageInquiryCreateRequest("BOOKING", "제목", "내용", "010-1234-5678"),
				null
			);

			assertThat(response.inquiryId()).isEqualTo(11L);
		}

		@Test
		@DisplayName("유효한 파일이면 문의를 등록할 수 있다")
		void createInquiry_유효파일_성공() {
			User user = OrderFixture.createUser();
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(inquiryRepository.save(any(Inquiry.class))).willAnswer(invocation -> {
				Inquiry inquiry = invocation.getArgument(0);
				ReflectionTestUtils.setField(inquiry, "id", 12L);
				return inquiry;
			});

			MockMultipartFile file = new MockMultipartFile(
				"file",
				"inquiry.jpg",
				"image/jpeg",
				new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF, 0x00}
			);

			MyPageInquiryCreateResponse response = myPageInquiryService.createInquiry(
				1L,
				new MyPageInquiryCreateRequest("BOOKING", "제목", "내용", null),
				file
			);

			assertThat(response.inquiryId()).isEqualTo(12L);
		}

		@Test
		@DisplayName("카테고리가 잘못되면 예외가 발생한다")
		void createInquiry_카테고리오류_예외() {
			User user = OrderFixture.createUser();
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);

			assertThatThrownBy(() -> myPageInquiryService.createInquiry(
				1L,
				new MyPageInquiryCreateRequest("INVALID", "제목", "내용", null),
				null
			))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_INQUIRY_CATEGORY));
		}

		@Test
		@DisplayName("허용되지 않은 확장자면 예외가 발생한다")
		void createInquiry_확장자오류_예외() {
			User user = OrderFixture.createUser();
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);

			MockMultipartFile file = new MockMultipartFile(
				"file",
				"inquiry.exe",
				"application/octet-stream",
				new byte[] {0x4D, 0x5A}
			);

			assertThatThrownBy(() -> myPageInquiryService.createInquiry(
				1L,
				new MyPageInquiryCreateRequest("BOOKING", "제목", "내용", null),
				file
			))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_FILE_TYPE_NOT_ALLOWED));
		}

		@Test
		@DisplayName("확장자와 시그니처가 다르면 예외가 발생한다")
		void createInquiry_시그니처불일치_예외() {
			User user = OrderFixture.createUser();
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);

			MockMultipartFile file = new MockMultipartFile(
				"file",
				"inquiry.jpg",
				"image/jpeg",
				new byte[] {0x4D, 0x5A, 0x00, 0x00}
			);

			assertThatThrownBy(() -> myPageInquiryService.createInquiry(
				1L,
				new MyPageInquiryCreateRequest("BOOKING", "제목", "내용", null),
				file
			))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_FILE_SIGNATURE_MISMATCH));
		}

		@Test
		@DisplayName("파일 크기 제한을 넘으면 예외가 발생한다")
		void createInquiry_파일크기초과_예외() {
			User user = OrderFixture.createUser();
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);

			byte[] oversized = new byte[5 * 1024 * 1024 + 1];
			oversized[0] = (byte)0xFF;
			oversized[1] = (byte)0xD8;
			oversized[2] = (byte)0xFF;
			MockMultipartFile file = new MockMultipartFile(
				"file",
				"inquiry.jpg",
				"image/jpeg",
				oversized
			);

			assertThatThrownBy(() -> myPageInquiryService.createInquiry(
				1L,
				new MyPageInquiryCreateRequest("BOOKING", "제목", "내용", null),
				file
			))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_FILE_TOO_LARGE));
		}
	}
}
