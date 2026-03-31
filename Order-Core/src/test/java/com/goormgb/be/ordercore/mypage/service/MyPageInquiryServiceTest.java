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
import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.inquiry.entity.Inquiry;
import com.goormgb.be.ordercore.inquiry.enums.InquiryCategory;
import com.goormgb.be.ordercore.inquiry.repository.InquiryRepository;
import com.goormgb.be.ordercore.mypage.dto.request.MyPageInquiryCreateRequest;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryCreateResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryDetailResponse;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyPageInquiryService 서비스 단위 테스트")
class MyPageInquiryServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private InquiryRepository inquiryRepository;
	@Mock
	private InquiryFileService inquiryFileService;

	private MyPageInquiryService myPageInquiryService;

	@BeforeEach
	void setUp() {
		myPageInquiryService = new MyPageInquiryService(
			userRepository,
			inquiryRepository,
			inquiryFileService
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
		@DisplayName("첨부파일이 있으면 downloadUrl을 포함해 조회된다")
		void getInquiryDetail_첨부있음_성공() {
			User user = OrderFixture.createUserWithId(1L);
			Inquiry inquiry = createInquiry(user, 12L, "dev/12/1/uuid.jpg");
			given(inquiryRepository.findById(12L)).willReturn(java.util.Optional.of(inquiry));
			given(inquiryFileService.generateDownloadUrl("dev/12/1/uuid.jpg"))
				.willReturn("https://signed.example.com/file");

			MyPageInquiryDetailResponse response = myPageInquiryService.getInquiryDetail(1L, 12L);

			assertThat(response.inquiryId()).isEqualTo(12L);
			assertThat(response.fileAttached()).isTrue();
			assertThat(response.downloadUrl()).isEqualTo("https://signed.example.com/file");
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
