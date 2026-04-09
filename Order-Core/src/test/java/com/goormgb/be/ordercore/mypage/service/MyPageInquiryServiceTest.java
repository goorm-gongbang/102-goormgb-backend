package com.goormgb.be.ordercore.mypage.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.List;

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
import com.goormgb.be.ordercore.inquiry.enums.InquiryStatus;
import com.goormgb.be.ordercore.inquiry.repository.InquiryRepository;
import com.goormgb.be.ordercore.mypage.dto.request.MyPageInquiryCreateRequest;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryCreateResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryDetailResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryListResponse;
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
			inquiryRepository
		);
	}

	@Nested
	@DisplayName("createInquiry")
	class CreateInquiry {

		@Test
		@DisplayName("문의를 등록할 수 있다")
		void createInquiry_성공() {
			User user = OrderFixture.createUser();
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(inquiryRepository.save(any(Inquiry.class))).willAnswer(invocation -> {
				Inquiry inquiry = invocation.getArgument(0);
				ReflectionTestUtils.setField(inquiry, "id", 11L);
				return inquiry;
			});

			MyPageInquiryCreateResponse response = myPageInquiryService.createInquiry(
				1L,
				new MyPageInquiryCreateRequest("BOOKING", "제목", "내용", "010-1234-5678")
			);

			assertThat(response.inquiryId()).isEqualTo(11L);
		}

		@Test
		@DisplayName("카테고리가 잘못되면 예외가 발생한다")
		void createInquiry_카테고리오류_예외() {
			User user = OrderFixture.createUser();
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);

			assertThatThrownBy(() -> myPageInquiryService.createInquiry(
				1L,
				new MyPageInquiryCreateRequest("INVALID", "제목", "내용", null)
			))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_INQUIRY_CATEGORY));
		}
	}

	@Nested
	@DisplayName("getInquiries")
	class GetInquiries {

		@Test
		@DisplayName("문의 목록을 최신순으로 매핑해서 반환한다")
		void getInquiries_성공() {
			User user = OrderFixture.createUserWithId(1L);
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			Inquiry first = createInquiry(user, 20L, "가장 최근 문의");
			first.updateStatus(InquiryStatus.ANSWERED);
			ReflectionTestUtils.setField(first, "createdAt", Instant.parse("2026-04-07T01:00:00Z"));

			Inquiry second = createInquiry(user, 10L, "이전 문의");
			ReflectionTestUtils.setField(second, "createdAt", Instant.parse("2026-04-06T01:00:00Z"));

			given(inquiryRepository.findAllByUserIdOrderByCreatedAtDesc(1L))
				.willReturn(List.of(first, second));

			MyPageInquiryListResponse response = myPageInquiryService.getInquiries(1L);

			assertThat(response.inquiries()).hasSize(2);
			assertThat(response.inquiries().get(0).inquiryId()).isEqualTo(20L);
			assertThat(response.inquiries().get(0).status()).isEqualTo("ANSWERED");
			assertThat(response.inquiries().get(1).inquiryId()).isEqualTo(10L);
		}

		@Test
		@DisplayName("문의가 없으면 빈 목록을 반환한다")
		void getInquiries_빈목록() {
			User user = OrderFixture.createUserWithId(1L);
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(inquiryRepository.findAllByUserIdOrderByCreatedAtDesc(1L))
				.willReturn(List.of());

			MyPageInquiryListResponse response = myPageInquiryService.getInquiries(1L);

			assertThat(response.inquiries()).isEmpty();
		}

		@Test
		@DisplayName("사용자가 없으면 예외가 발생한다")
		void getInquiries_사용자없음_예외() {
			given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			assertThatThrownBy(() -> myPageInquiryService.getInquiries(1L))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.USER_NOT_FOUND));
		}

		private Inquiry createInquiry(User user, Long inquiryId, String title) {
			Inquiry inquiry = Inquiry.create(
				user,
				InquiryCategory.BOOKING,
				title,
				"내용",
				"010-1234-5678"
			);
			ReflectionTestUtils.setField(inquiry, "id", inquiryId);
			return inquiry;
		}
	}

	@Nested
	@DisplayName("getInquiryDetail")
	class GetInquiryDetail {

		@Test
		@DisplayName("첨부파일이 없으면 downloadUrl 없이 조회된다")
		void getInquiryDetail_첨부없음_성공() {
			User user = OrderFixture.createUserWithId(1L);
			Inquiry inquiry = createInquiry(user, 11L, null);
			given(inquiryRepository.findById(11L)).willReturn(java.util.Optional.of(inquiry));

			MyPageInquiryDetailResponse response = myPageInquiryService.getInquiryDetail(1L, 11L);

			assertThat(response.inquiryId()).isEqualTo(11L);
			assertThat(response.fileAttached()).isFalse();
			assertThat(response.downloadUrl()).isNull();
		}

		@Test
		@DisplayName("첨부파일이 있어도 fileAttached=false, downloadUrl=null로 반환된다 (파일 업로드 비활성화)")
		void getInquiryDetail_첨부있음_downloadUrl_null() {
			User user = OrderFixture.createUserWithId(1L);
			Inquiry inquiry = createInquiry(user, 12L, "dev/12/1/uuid.jpg");
			given(inquiryRepository.findById(12L)).willReturn(java.util.Optional.of(inquiry));

			MyPageInquiryDetailResponse response = myPageInquiryService.getInquiryDetail(1L, 12L);

			assertThat(response.inquiryId()).isEqualTo(12L);
			assertThat(response.fileAttached()).isFalse();
			assertThat(response.downloadUrl()).isNull();
		}

		@Test
		@DisplayName("없는 문의면 예외가 발생한다")
		void getInquiryDetail_문의없음_예외() {
			given(inquiryRepository.findById(99L)).willReturn(java.util.Optional.empty());

			assertThatThrownBy(() -> myPageInquiryService.getInquiryDetail(1L, 99L))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_NOT_FOUND));
		}

		@Test
		@DisplayName("타인 문의면 예외가 발생한다")
		void getInquiryDetail_권한없음_예외() {
			User owner = OrderFixture.createUserWithId(2L);
			Inquiry inquiry = createInquiry(owner, 13L, null);
			given(inquiryRepository.findById(13L)).willReturn(java.util.Optional.of(inquiry));

			assertThatThrownBy(() -> myPageInquiryService.getInquiryDetail(1L, 13L))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INQUIRY_ACCESS_DENIED));
		}

		private Inquiry createInquiry(User user, Long inquiryId, String fileKey) {
			Inquiry inquiry = Inquiry.create(
				user,
				InquiryCategory.BOOKING,
				"제목",
				"내용",
				"010-1234-5678"
			);
			ReflectionTestUtils.setField(inquiry, "id", inquiryId);
			if (fileKey != null) {
				inquiry.updateFileKey(fileKey);
			}
			return inquiry;
		}
	}
}
