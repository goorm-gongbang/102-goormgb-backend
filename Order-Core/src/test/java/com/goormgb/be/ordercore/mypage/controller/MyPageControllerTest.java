package com.goormgb.be.ordercore.mypage.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
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
import com.goormgb.be.ordercore.mypage.dto.request.MyPageAccountUpdateRequest;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageAccountResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryCreateResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryDetailResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryListResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketCancelResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketDetailResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketQrResponse;
import com.goormgb.be.ordercore.mypage.service.MyPageInquiryService;
import com.goormgb.be.ordercore.mypage.service.MyPageProfileService;
import com.goormgb.be.ordercore.mypage.service.MyPageTicketService;
import com.goormgb.be.ordercore.support.WebMvcTestSupport;

@WebMvcTest(controllers = MyPageController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MyPageController 슬라이스 테스트")
class MyPageControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private MyPageProfileService myPageProfileService;

	@MockitoBean
	private MyPageTicketService myPageTicketService;

	@MockitoBean
	private MyPageInquiryService myPageInquiryService;

	private void setAuthentication(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")))
		);
	}

	@Nested
	@DisplayName("GET /mypage/account — 개인정보 조회")
	class GetAccount {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 요청이면 200과 계정 정보를 반환한다")
		void getAccount_성공() throws Exception {
			MyPageAccountResponse response = MyPageFixture.createAccountResponse();
			given(myPageProfileService.getAccount(1L)).willReturn(response);

			mockMvc.perform(get("/mypage/account"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("조회 성공"))
				.andExpect(jsonPath("$.data.email").value("user@example.com"))
				.andExpect(jsonPath("$.data.nickname").value("goorm_new"))
				.andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.goormgb.com/profile/user-1.png"))
				.andExpect(jsonPath("$.data.snsAccount.provider").value("KAKAO"));
		}

		@Test
		@DisplayName("사용자가 없으면 404를 반환한다")
		void getAccount_사용자없음_404() throws Exception {
			given(myPageProfileService.getAccount(1L))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			mockMvc.perform(get("/mypage/account"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
		}
	}

	@Nested
	@DisplayName("PUT /mypage/account — 개인정보 수정")
	class UpdateAccount {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 요청이면 200과 수정된 계정 정보를 반환한다")
		void updateAccount_성공() throws Exception {
			MyPageAccountResponse response = MyPageFixture.createAccountResponse();
			given(myPageProfileService.updateAccount(eq(1L), any(MyPageAccountUpdateRequest.class))).willReturn(
				response);

			mockMvc.perform(put("/mypage/account")
					.contentType("application/json")
					.content("""
						{
						  "nickname": "goorm_new"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("수정 성공"))
				.andExpect(jsonPath("$.data.email").value("user@example.com"))
				.andExpect(jsonPath("$.data.nickname").value("goorm_new"))
				.andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.goormgb.com/profile/user-1.png"))
				.andExpect(jsonPath("$.data.snsAccount.provider").value("KAKAO"));
		}

		@Test
		@DisplayName("닉네임이 유효하지 않으면 400을 반환한다")
		void updateAccount_닉네임오류_400() throws Exception {
			mockMvc.perform(put("/mypage/account")
					.contentType("application/json")
					.content("""
						{
						  "nickname": "   "
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("nickname: 닉네임은 공백일 수 없습니다."));
		}

		@Test
		@DisplayName("사용자가 없으면 404를 반환한다")
		void updateAccount_사용자없음_404() throws Exception {
			given(myPageProfileService.updateAccount(eq(1L), any(MyPageAccountUpdateRequest.class)))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			mockMvc.perform(put("/mypage/account")
					.contentType("application/json")
					.content("""
						{
						  "nickname": "goorm_new"
						}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
		}
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
			given(myPageProfileService.getProfile(1L)).willReturn(response);

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
			given(myPageProfileService.getProfile(any()))
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
			given(myPageTicketService.getTickets(eq(1L), eq("BOOKED"), eq(0), eq(10))).willReturn(response);

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
			given(myPageTicketService.getTickets(eq(1L), eq("CANCEL_REFUND"), eq(0), eq(10))).willReturn(response);

			mockMvc.perform(get("/mypage/tickets")
					.param("tab", "CANCEL_REFUND"))
				.andExpect(status().isOk());
		}

		@Test
		@DisplayName("page와 size 파라미터를 명시적으로 지정할 수 있다")
		void getTickets_페이지네이션_파라미터_지정() throws Exception {
			MyPageTicketListResponse response = MyPageFixture.createTicketListResponse();
			given(myPageTicketService.getTickets(eq(1L), eq("BOOKED"), eq(1), eq(5))).willReturn(response);

			mockMvc.perform(get("/mypage/tickets")
					.param("page", "1")
					.param("size", "5"))
				.andExpect(status().isOk());
		}

		@Test
		@DisplayName("size가 10을 초과하면 400을 반환한다")
		void getTickets_size초과_400() throws Exception {
			given(myPageTicketService.getTickets(any(), any(), anyInt(), anyInt()))
				.willThrow(new CustomException(ErrorCode.INVALID_PAGE_SIZE));

			mockMvc.perform(get("/mypage/tickets")
					.param("size", "11"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("size는 최대 10까지 허용됩니다."));
		}

		@Test
		@DisplayName("유효하지 않은 탭 값이면 400을 반환한다")
		void getTickets_잘못된탭_400() throws Exception {
			given(myPageTicketService.getTickets(any(), eq("INVALID"), anyInt(), anyInt()))
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
			given(myPageTicketService.getTickets(eq(1L), eq("BOOKED"), eq(0), eq(10))).willReturn(emptyResponse);

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
			given(myPageTicketService.getTickets(eq(1L), eq("BOOKED"), eq(0), eq(10))).willReturn(response);

			mockMvc.perform(get("/mypage/tickets"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tickets[0].actions.canDeposit").value(false))
				.andExpect(jsonPath("$.data.tickets[0].actions.canCancel").value(true))
				.andExpect(jsonPath("$.data.tickets[0].actions.canViewDetail").value(true));
		}
	}

	@Nested
	@DisplayName("GET /mypage/tickets/{ticketId} — 예매 상세 조회")
	class GetTicketDetail {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 요청이면 200과 상세 정보를 반환한다")
		void getTicketDetail_성공() throws Exception {
			MyPageTicketDetailResponse response = MyPageFixture.createTicketDetailResponse();
			given(myPageTicketService.getTicketDetail(1L, 101L)).willReturn(response);

			mockMvc.perform(get("/mypage/tickets/101"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("조회 성공"))
				.andExpect(jsonPath("$.data.ticketId").value(101))
				.andExpect(jsonPath("$.data.status").value("PAID"))
				.andExpect(jsonPath("$.data.actions.canPrint").value(true))
				.andExpect(jsonPath("$.data.match.matchId").value(55))
				.andExpect(jsonPath("$.data.seats.length()").value(2));
		}

		@Test
		@DisplayName("존재하지 않는 ticketId면 404를 반환한다")
		void getTicketDetail_주문없음_404() throws Exception {
			given(myPageTicketService.getTicketDetail(1L, 999L))
				.willThrow(new CustomException(ErrorCode.ORDER_NOT_FOUND));

			mockMvc.perform(get("/mypage/tickets/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."));
		}

		@Test
		@DisplayName("본인 소유가 아니면 404를 반환한다")
		void getTicketDetail_권한없음_404() throws Exception {
			given(myPageTicketService.getTicketDetail(1L, 101L))
				.willThrow(new CustomException(ErrorCode.ORDER_NOT_FOUND));

			mockMvc.perform(get("/mypage/tickets/101"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."));
		}
	}

	@Nested
	@DisplayName("GET /mypage/tickets/{ticketId}/qr — 입장용 QR 조회")
	class GetTicketEntryQr {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 요청이면 200과 QR 정보를 반환한다")
		void getTicketEntryQr_성공() throws Exception {
			MyPageTicketQrResponse response = MyPageFixture.createTicketQrResponse();
			given(myPageTicketService.getTicketEntryQr(1L, 101L)).willReturn(response);

			mockMvc.perform(get("/mypage/tickets/101/qr"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("QR 발급 성공"))
				.andExpect(jsonPath("$.data.ticketId").value(101))
				.andExpect(jsonPath("$.data.qrToken").value("qr-token-uuid"))
				.andExpect(jsonPath("$.data.match.homeClub.koName").value("LG 트윈스"))
				.andExpect(jsonPath("$.data.seats.length()").value(2));
		}

		@Test
		@DisplayName("본인 소유가 아니면 404를 반환한다")
		void getTicketEntryQr_권한없음_404() throws Exception {
			given(myPageTicketService.getTicketEntryQr(1L, 101L))
				.willThrow(new CustomException(ErrorCode.ORDER_NOT_FOUND));

			mockMvc.perform(get("/mypage/tickets/101/qr"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."));
		}

		@Test
		@DisplayName("입장 가능 시간이 아니면 400을 반환한다")
		void getTicketEntryQr_입장시간아님_400() throws Exception {
			given(myPageTicketService.getTicketEntryQr(1L, 101L))
				.willThrow(new CustomException(ErrorCode.ENTRY_QR_NOT_AVAILABLE_YET));

			mockMvc.perform(get("/mypage/tickets/101/qr"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("아직 입장 가능 시간이 아닙니다."));
		}

		@Test
		@DisplayName("경기 시작 이후면 400을 반환한다")
		void getTicketEntryQr_경기시작이후_400() throws Exception {
			given(myPageTicketService.getTicketEntryQr(1L, 101L))
				.willThrow(new CustomException(ErrorCode.ENTRY_QR_MATCH_STARTED));

			mockMvc.perform(get("/mypage/tickets/101/qr"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("경기 시작 이후에는 QR을 발급할 수 없습니다."));
		}
	}

	@Nested
	@DisplayName("POST /mypage/tickets/{ticketId}/cancel — 티켓 취소 요청")
	class RequestTicketCancel {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 요청이면 200과 취소 결과를 반환한다")
		void requestTicketCancel_성공() throws Exception {
			MyPageTicketCancelResponse response = MyPageFixture.createTicketCancelResponse();
			given(myPageTicketService.requestTicketCancel(1L, 101L)).willReturn(response);

			mockMvc.perform(post("/mypage/tickets/101/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("취소 요청이 완료되었습니다."))
				.andExpect(jsonPath("$.data.ticketId").value(101))
				.andExpect(jsonPath("$.data.status").value("CANCEL_REQUESTED"))
				.andExpect(jsonPath("$.data.totalAmount").value(42000))
				.andExpect(jsonPath("$.data.cancellationFee").value(6000))
				.andExpect(jsonPath("$.data.refundedAmount").value(36000));
		}

		@Test
		@DisplayName("본인 소유가 아니면 404를 반환한다")
		void requestTicketCancel_권한없음_404() throws Exception {
			given(myPageTicketService.requestTicketCancel(1L, 101L))
				.willThrow(new CustomException(ErrorCode.ORDER_NOT_FOUND));

			mockMvc.perform(post("/mypage/tickets/101/cancel"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."));
		}

		@Test
		@DisplayName("취소 가능한 기간이 아니면 400을 반환한다")
		void requestTicketCancel_취소불가기간_400() throws Exception {
			given(myPageTicketService.requestTicketCancel(1L, 101L))
				.willThrow(new CustomException(ErrorCode.TICKET_CANCEL_NOT_ALLOWED));

			mockMvc.perform(post("/mypage/tickets/101/cancel"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("취소 가능한 기간이 아닙니다."));
		}
	}

	@Nested
	@DisplayName("GET /mypage/inquiries — 1:1 문의 목록 조회")
	class GetInquiries {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 요청이면 200과 문의 목록을 반환한다")
		void getInquiries_성공() throws Exception {
			given(myPageInquiryService.getInquiries(1L))
				.willReturn(new MyPageInquiryListResponse(
					List.of(
						new MyPageInquiryListResponse.InquiryItem(
							11L,
							"BOOKING",
							"문의 제목",
							"REGISTERED",
							Instant.parse("2026-03-31T08:00:00Z")
						)
					)
				));

			mockMvc.perform(get("/mypage/inquiries"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("조회 성공"))
				.andExpect(jsonPath("$.data.inquiries").isArray())
				.andExpect(jsonPath("$.data.inquiries.length()").value(1))
				.andExpect(jsonPath("$.data.inquiries[0].inquiryId").value(11))
				.andExpect(jsonPath("$.data.inquiries[0].category").value("BOOKING"))
				.andExpect(jsonPath("$.data.inquiries[0].title").value("문의 제목"))
				.andExpect(jsonPath("$.data.inquiries[0].status").value("REGISTERED"));
		}

		@Test
		@DisplayName("문의가 없으면 빈 배열을 반환한다")
		void getInquiries_빈목록_반환() throws Exception {
			given(myPageInquiryService.getInquiries(1L))
				.willReturn(new MyPageInquiryListResponse(List.of()));

			mockMvc.perform(get("/mypage/inquiries"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.inquiries").isArray())
				.andExpect(jsonPath("$.data.inquiries.length()").value(0));
		}

		@Test
		@DisplayName("사용자가 없으면 404를 반환한다")
		void getInquiries_사용자없음_404() throws Exception {
			given(myPageInquiryService.getInquiries(1L))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			mockMvc.perform(get("/mypage/inquiries"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
		}
	}

	@Nested
	@DisplayName("POST /mypage/inquiries — 1:1 문의 작성")
	class CreateInquiry {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 JSON 요청이면 201과 inquiryId를 반환한다")
		void createInquiry_성공() throws Exception {
			given(myPageInquiryService.createInquiry(eq(1L), any()))
				.willReturn(MyPageInquiryCreateResponse.of(11L));

			mockMvc.perform(post("/mypage/inquiries")
					.contentType("application/json")
					.content("""
						{
						  "category": "BOOKING",
						  "title": "좌석 변경 문의",
						  "content": "좌석 변경이 가능한지 확인 부탁드립니다.",
						  "phoneNumber": "010-1234-5678"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("CREATED"))
				.andExpect(jsonPath("$.message").value("문의가 등록되었습니다."))
				.andExpect(jsonPath("$.data.inquiryId").value(11));
		}

		@Test
		@DisplayName("유효하지 않은 카테고리면 400을 반환한다")
		void createInquiry_카테고리오류_400() throws Exception {
			given(myPageInquiryService.createInquiry(eq(1L), any()))
				.willThrow(new CustomException(ErrorCode.INVALID_INQUIRY_CATEGORY));

			mockMvc.perform(post("/mypage/inquiries")
					.contentType("application/json")
					.content("""
						{
						  "category": "INVALID",
						  "title": "좌석 변경 문의",
						  "content": "문의 내용"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("유효하지 않은 문의 카테고리입니다."));
		}

		@Test
		@DisplayName("필수 필드가 없으면 400을 반환한다")
		void createInquiry_필수필드누락_400() throws Exception {
			mockMvc.perform(post("/mypage/inquiries")
					.contentType("application/json")
					.content("""
						{
						  "category": "BOOKING",
						  "content": "문의 내용"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("title: title은 필수입니다."));
		}
	}

	@Nested
	@DisplayName("GET /mypage/inquiries/{inquiryId} — 문의 상세 조회")
	class GetInquiryDetail {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 요청이면 200과 문의 상세를 반환한다")
		void getInquiryDetail_성공() throws Exception {
			given(myPageInquiryService.getInquiryDetail(1L, 11L))
				.willReturn(new MyPageInquiryDetailResponse(
					11L,
					"BOOKING",
					"문의 제목",
					"문의 내용",
					"010-1234-5678",
					"REGISTERED",
					true,
					"https://signed.example.com/get",
					Instant.parse("2026-03-31T08:00:00Z")
				));

			mockMvc.perform(get("/mypage/inquiries/11"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("조회 성공"))
				.andExpect(jsonPath("$.data.inquiryId").value(11))
				.andExpect(jsonPath("$.data.fileAttached").value(true))
				.andExpect(jsonPath("$.data.downloadUrl").value("https://signed.example.com/get"));
		}

		@Test
		@DisplayName("타인 문의면 403을 반환한다")
		void getInquiryDetail_권한없음_403() throws Exception {
			given(myPageInquiryService.getInquiryDetail(1L, 11L))
				.willThrow(new CustomException(ErrorCode.INQUIRY_ACCESS_DENIED));

			mockMvc.perform(get("/mypage/inquiries/11"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("해당 문의에 접근할 권한이 없습니다."));
		}
	}
}
