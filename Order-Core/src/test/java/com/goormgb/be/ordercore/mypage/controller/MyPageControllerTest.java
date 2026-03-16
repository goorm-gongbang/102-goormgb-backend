package com.goormgb.be.ordercore.mypage.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.mypage.MyPageFixture;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.service.MyPageService;
import com.goormgb.be.ordercore.support.WebMvcTestSupport;

@WebMvcTest(controllers = MyPageController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MyPageController 슬라이스 테스트")
class MyPageControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private MyPageService myPageService;

	private void setAuthentication(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")))
		);
	}

	@Nested
	@DisplayName("GET /mypage/profile — 프로필 요약 조회")
	class GetProfile {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 요청이면 200과 프로필 정보를 반환한다")
		void getProfile_성공() throws Exception {
			MyPageProfileResponse response = MyPageFixture.createProfileResponse();
			given(myPageService.getProfile(1L)).willReturn(response);

			mockMvc.perform(get("/mypage/profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("조회 성공"))
				.andExpect(jsonPath("$.data.profile.nickname").value("goorm123"))
				.andExpect(jsonPath("$.data.profile.snsProvider").value("KAKAO"))
				.andExpect(jsonPath("$.data.ticketSummary.upcomingCount").value(2))
				.andExpect(jsonPath("$.data.ticketSummary.cancelRefundCount").value(1))
				.andExpect(jsonPath("$.data.ticketSummary.completedCount").value(5));
		}

		@Test
		@DisplayName("사용자가 없으면 404를 반환한다")
		void getProfile_사용자_미발견_404() throws Exception {
			given(myPageService.getProfile(any()))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			mockMvc.perform(get("/mypage/profile"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
		}
	}

	@Nested
	@DisplayName("GET /mypage/tickets — 예매 내역 목록 조회")
	class GetTickets {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("기본 파라미터로 예매 내역 목록을 반환한다")
		void getTickets_기본파라미터_성공() throws Exception {
			MyPageTicketListResponse response = MyPageFixture.createTicketListResponse();
			given(myPageService.getTickets(eq(1L), eq("BOOKED"), eq(0), eq(10))).willReturn(response);

			mockMvc.perform(get("/mypage/tickets"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.data.currentTab").value("BOOKED"))
				.andExpect(jsonPath("$.data.summary.totalCount").value(8))
				.andExpect(jsonPath("$.data.pagination.page").value(0))
				.andExpect(jsonPath("$.data.pagination.size").value(10))
				.andExpect(jsonPath("$.data.tickets").isArray())
				.andExpect(jsonPath("$.data.tickets.length()").value(1))
				.andExpect(jsonPath("$.data.tickets[0].ticketId").value(101));
		}

		@Test
		@DisplayName("CANCEL_REFUND 탭으로 조회할 수 있다")
		void getTickets_CANCEL_REFUND탭_성공() throws Exception {
			MyPageTicketListResponse response = MyPageFixture.createTicketListResponse();
			given(myPageService.getTickets(eq(1L), eq("CANCEL_REFUND"), eq(0), eq(10))).willReturn(response);

			mockMvc.perform(get("/mypage/tickets")
					.param("tab", "CANCEL_REFUND"))
				.andExpect(status().isOk());
		}

		@Test
		@DisplayName("page와 size 파라미터를 명시적으로 지정할 수 있다")
		void getTickets_페이지네이션_파라미터_지정() throws Exception {
			MyPageTicketListResponse response = MyPageFixture.createTicketListResponse();
			given(myPageService.getTickets(eq(1L), eq("BOOKED"), eq(1), eq(5))).willReturn(response);

			mockMvc.perform(get("/mypage/tickets")
					.param("page", "1")
					.param("size", "5"))
				.andExpect(status().isOk());
		}

		@Test
		@DisplayName("size가 10을 초과하면 400을 반환한다")
		void getTickets_size초과_400() throws Exception {
			given(myPageService.getTickets(any(), any(), anyInt(), anyInt()))
				.willThrow(new CustomException(ErrorCode.INVALID_PAGE_SIZE));

			mockMvc.perform(get("/mypage/tickets")
					.param("size", "11"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("size는 최대 10까지 허용됩니다."));
		}

		@Test
		@DisplayName("유효하지 않은 탭 값이면 400을 반환한다")
		void getTickets_잘못된탭_400() throws Exception {
			given(myPageService.getTickets(any(), eq("INVALID"), anyInt(), anyInt()))
				.willThrow(new CustomException(ErrorCode.INVALID_TICKET_TAB));

			mockMvc.perform(get("/mypage/tickets")
					.param("tab", "INVALID"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("유효하지 않은 탭 값입니다."));
		}

		@Test
		@DisplayName("티켓 목록이 비어있으면 빈 배열과 pagination 정보를 반환한다")
		void getTickets_빈목록_반환() throws Exception {
			MyPageTicketListResponse emptyResponse = MyPageTicketListResponse.of(
				0, 0, 0, 0, "BOOKED", 0, 10, 0L, 0, false, List.of()
			);
			given(myPageService.getTickets(eq(1L), eq("BOOKED"), eq(0), eq(10))).willReturn(emptyResponse);

			mockMvc.perform(get("/mypage/tickets"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tickets").isArray())
				.andExpect(jsonPath("$.data.tickets.length()").value(0))
				.andExpect(jsonPath("$.data.pagination.totalElements").value(0))
				.andExpect(jsonPath("$.data.pagination.hasNext").value(false));
		}

		@Test
		@DisplayName("티켓 actions 필드가 올바르게 반환된다")
		void getTickets_actions_필드_확인() throws Exception {
			MyPageTicketListResponse response = MyPageFixture.createTicketListResponse();
			given(myPageService.getTickets(eq(1L), eq("BOOKED"), eq(0), eq(10))).willReturn(response);

			mockMvc.perform(get("/mypage/tickets"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tickets[0].actions.canDeposit").value(false))
				.andExpect(jsonPath("$.data.tickets[0].actions.canCancel").value(true))
				.andExpect(jsonPath("$.data.tickets[0].actions.canViewDetail").value(true));
		}
	}
}
