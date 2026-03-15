package com.goormgb.be.ordercore.payment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.payment.PaymentFixture;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.payment.dto.request.CashReceiptCreateRequest;
import com.goormgb.be.ordercore.payment.dto.request.PaymentProcessRequest;
import com.goormgb.be.ordercore.payment.dto.response.CashReceiptCreateResponse;
import com.goormgb.be.ordercore.payment.dto.response.PaymentProcessResponse;
import com.goormgb.be.ordercore.payment.enums.CashReceiptPurpose;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;
import com.goormgb.be.ordercore.payment.enums.PaymentStatus;
import com.goormgb.be.ordercore.payment.service.PaymentService;
import com.goormgb.be.ordercore.support.WebMvcTestSupport;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PaymentController 슬라이스 테스트")
class PaymentControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private PaymentService paymentService;

	private void setAuthentication(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")))
		);
	}

	@Nested
	@DisplayName("POST /mypage/orders/{orderId}/payment — 결제 처리")
	class ProcessPayment {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("토스페이 결제 성공 시 200과 COMPLETED 상태를 반환한다")
		void processPayment_TOSS_PAY_성공() throws Exception {
			PaymentProcessRequest request = PaymentFixture.createTossPayRequest();
			PaymentProcessResponse response = new PaymentProcessResponse(
				1L, OrderStatus.PAID,
				PaymentMethod.TOSS_PAY, PaymentStatus.COMPLETED,
				Instant.now(), null
			);

			given(paymentService.processPayment(eq(1L), eq(1L), any(PaymentProcessRequest.class)))
				.willReturn(response);

			mockMvc.perform(post("/mypage/orders/1/payment")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.data.orderId").value(1))
				.andExpect(jsonPath("$.data.orderStatus").value("PAID"))
				.andExpect(jsonPath("$.data.paymentMethod").value("TOSS_PAY"))
				.andExpect(jsonPath("$.data.paymentStatus").value("COMPLETED"))
				.andExpect(jsonPath("$.data.paidAt").exists());
		}

		@Test
		@DisplayName("카카오페이 결제 성공 시 200과 COMPLETED 상태를 반환한다")
		void processPayment_KAKAO_PAY_성공() throws Exception {
			PaymentProcessRequest request = PaymentFixture.createKakaoPayRequest();
			PaymentProcessResponse response = new PaymentProcessResponse(
				1L, OrderStatus.PAID,
				PaymentMethod.KAKAO_PAY, PaymentStatus.COMPLETED,
				Instant.now(), null
			);

			given(paymentService.processPayment(eq(1L), eq(1L), any(PaymentProcessRequest.class)))
				.willReturn(response);

			mockMvc.perform(post("/mypage/orders/1/payment")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.paymentMethod").value("KAKAO_PAY"))
				.andExpect(jsonPath("$.data.paymentStatus").value("COMPLETED"));
		}

		@Test
		@DisplayName("가상계좌 결제 성공 시 200과 가상계좌 정보를 반환한다")
		void processPayment_VIRTUAL_ACCOUNT_성공() throws Exception {
			PaymentProcessRequest request = PaymentFixture.createVirtualAccountRequest();
			Instant deadline = Instant.now().plus(3, ChronoUnit.DAYS);
			PaymentProcessResponse.VirtualAccountInfo vaInfo = new PaymentProcessResponse.VirtualAccountInfo(
				"국민은행", "047-000-00000001", "구름GB", deadline
			);
			PaymentProcessResponse response = new PaymentProcessResponse(
				1L, OrderStatus.PAYMENT_PENDING,
				PaymentMethod.VIRTUAL_ACCOUNT, PaymentStatus.PENDING,
				null, vaInfo
			);

			given(paymentService.processPayment(eq(1L), eq(1L), any(PaymentProcessRequest.class)))
				.willReturn(response);

			mockMvc.perform(post("/mypage/orders/1/payment")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.paymentMethod").value("VIRTUAL_ACCOUNT"))
				.andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
				.andExpect(jsonPath("$.data.orderStatus").value("PAYMENT_PENDING"))
				.andExpect(jsonPath("$.data.virtualAccount.bank").value("국민은행"))
				.andExpect(jsonPath("$.data.virtualAccount.holder").value("구름GB"))
				.andExpect(jsonPath("$.data.virtualAccount.accountNumber").value("047-000-00000001"));
		}

		@Test
		@DisplayName("paymentMethod가 없으면 400을 반환한다")
		void processPayment_결제수단_누락_400() throws Exception {
			mockMvc.perform(post("/mypage/orders/1/payment")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("주문이 없으면 404를 반환한다")
		void processPayment_주문_미발견_404() throws Exception {
			given(paymentService.processPayment(any(), any(), any()))
				.willThrow(new CustomException(ErrorCode.ORDER_NOT_FOUND));

			mockMvc.perform(post("/mypage/orders/99/payment")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(PaymentFixture.createTossPayRequest())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."));
		}

		@Test
		@DisplayName("주문 소유권이 없으면 403을 반환한다")
		void processPayment_소유권_없음_403() throws Exception {
			given(paymentService.processPayment(any(), any(), any()))
				.willThrow(new CustomException(ErrorCode.ORDER_ACCESS_DENIED));

			mockMvc.perform(post("/mypage/orders/1/payment")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(PaymentFixture.createTossPayRequest())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("해당 주문에 접근할 권한이 없습니다."));
		}

		@Test
		@DisplayName("이미 결제 완료된 주문이면 400을 반환한다")
		void processPayment_이미_완료_400() throws Exception {
			given(paymentService.processPayment(any(), any(), any()))
				.willThrow(new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED));

			mockMvc.perform(post("/mypage/orders/1/payment")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(PaymentFixture.createTossPayRequest())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("이미 결제가 완료된 주문입니다."));
		}
	}

	@Nested
	@DisplayName("POST /mypage/orders/{orderId}/cash-receipt — 현금영수증 신청")
	class CreateCashReceipt {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("개인소득공제 현금영수증 신청 성공 시 201을 반환한다")
		void createCashReceipt_개인소득공제_성공() throws Exception {
			CashReceiptCreateRequest request = PaymentFixture.createPersonalDeductionRequest();
			CashReceiptCreateResponse response = new CashReceiptCreateResponse(
				1L, CashReceiptPurpose.PERSONAL_DEDUCTION, "010-1234-5678"
			);

			given(paymentService.createCashReceipt(eq(1L), eq(1L), any(CashReceiptCreateRequest.class)))
				.willReturn(response);

			mockMvc.perform(post("/mypage/orders/1/cash-receipt")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.data.orderId").value(1))
				.andExpect(jsonPath("$.data.purpose").value("PERSONAL_DEDUCTION"))
				.andExpect(jsonPath("$.data.number").value("010-1234-5678"));
		}

		@Test
		@DisplayName("사업자지출증빙 현금영수증 신청 성공 시 201을 반환한다")
		void createCashReceipt_사업자지출증빙_성공() throws Exception {
			CashReceiptCreateRequest request = PaymentFixture.createBusinessExpenseRequest();
			CashReceiptCreateResponse response = new CashReceiptCreateResponse(
				1L, CashReceiptPurpose.BUSINESS_EXPENSE, "123-45-67890"
			);

			given(paymentService.createCashReceipt(eq(1L), eq(1L), any(CashReceiptCreateRequest.class)))
				.willReturn(response);

			mockMvc.perform(post("/mypage/orders/1/cash-receipt")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.purpose").value("BUSINESS_EXPENSE"))
				.andExpect(jsonPath("$.data.number").value("123-45-67890"));
		}

		@Test
		@DisplayName("purpose가 없으면 400을 반환한다")
		void createCashReceipt_purpose_누락_400() throws Exception {
			mockMvc.perform(post("/mypage/orders/1/cash-receipt")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"number\": \"010-1234-5678\"}"))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("number가 없으면 400을 반환한다")
		void createCashReceipt_number_누락_400() throws Exception {
			mockMvc.perform(post("/mypage/orders/1/cash-receipt")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"purpose\": \"PERSONAL_DEDUCTION\"}"))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("결제 정보가 없으면 404를 반환한다")
		void createCashReceipt_결제_미발견_404() throws Exception {
			given(paymentService.createCashReceipt(any(), any(), any()))
				.willThrow(new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

			mockMvc.perform(post("/mypage/orders/1/cash-receipt")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(PaymentFixture.createPersonalDeductionRequest())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("결제 정보를 찾을 수 없습니다."));
		}

		@Test
		@DisplayName("현금영수증이 이미 신청된 경우 409를 반환한다")
		void createCashReceipt_중복신청_409() throws Exception {
			given(paymentService.createCashReceipt(any(), any(), any()))
				.willThrow(new CustomException(ErrorCode.CASH_RECEIPT_ALREADY_EXISTS));

			mockMvc.perform(post("/mypage/orders/1/cash-receipt")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(PaymentFixture.createPersonalDeductionRequest())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("현금영수증이 이미 신청되었습니다."));
		}

		@Test
		@DisplayName("주문 소유권이 없으면 403을 반환한다")
		void createCashReceipt_소유권_없음_403() throws Exception {
			given(paymentService.createCashReceipt(any(), any(), any()))
				.willThrow(new CustomException(ErrorCode.ORDER_ACCESS_DENIED));

			mockMvc.perform(post("/mypage/orders/1/cash-receipt")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(PaymentFixture.createPersonalDeductionRequest())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("해당 주문에 접근할 권한이 없습니다."));
		}

		@Test
		@DisplayName("주문이 없으면 404를 반환한다")
		void createCashReceipt_주문_미발견_404() throws Exception {
			given(paymentService.createCashReceipt(any(), any(), any()))
				.willThrow(new CustomException(ErrorCode.ORDER_NOT_FOUND));

			mockMvc.perform(post("/mypage/orders/1/cash-receipt")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(PaymentFixture.createPersonalDeductionRequest())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."));
		}
	}
}
