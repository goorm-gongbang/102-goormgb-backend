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
import com.goormgb.be.ordercore.inquiry.repository.InquiryRepository;
import com.goormgb.be.ordercore.mypage.dto.request.MyPageInquiryCreateRequest;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryCreateResponse;
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
}
