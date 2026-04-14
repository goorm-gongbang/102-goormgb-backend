package com.goormgb.be.ordercore.email.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.kafka.event.PaymentCompletedEvent;
import com.goormgb.be.ordercore.email.service.EmailDataQueryService;
import com.goormgb.be.ordercore.email.service.EmailService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailEventConsumer 단위 테스트")
class EmailEventConsumerTest {

	@Mock
	private EmailService emailService;
	@Mock
	private EmailDataQueryService emailDataQueryService;

	@Test
	@DisplayName("주문이 PAID이면 booking과 payment 메일을 모두 발송한다")
	void handlePaymentCompleted_paidOrder_sendsBookingAndPayment() {
		EmailEventConsumer consumer = new EmailEventConsumer(emailService, emailDataQueryService);
		PaymentCompletedEvent event = PaymentCompletedEvent.builder()
			.orderId(1L)
			.occurredAt(Instant.now())
			.paymentMethod("TOSS_PAY")
			.build();

		Map<String, Object> bookingContext = new HashMap<>();
		Map<String, Object> paymentContext = new HashMap<>();

		given(emailDataQueryService.isOrderPaid(1L)).willReturn(true);
		given(emailDataQueryService.buildBookingEmailContext(1L, event)).willReturn(Optional.of(bookingContext));
		given(emailDataQueryService.buildPaymentEmailContext(1L, event)).willReturn(Optional.of(paymentContext));

		consumer.handlePaymentCompleted(event);

		then(emailService).should().sendBookingConfirmation(bookingContext);
		then(emailService).should().sendPaymentConfirmation(paymentContext);
	}

	@Test
	@DisplayName("주문이 PAID가 아니면 booking 메일을 발송하지 않고 payment 메일만 발송한다")
	void handlePaymentCompleted_notPaid_skipsBooking() {
		EmailEventConsumer consumer = new EmailEventConsumer(emailService, emailDataQueryService);
		PaymentCompletedEvent event = PaymentCompletedEvent.builder()
			.orderId(2L)
			.occurredAt(Instant.now())
			.paymentMethod("BANK_TRANSFER")
			.build();

		Map<String, Object> paymentContext = new HashMap<>();

		given(emailDataQueryService.isOrderPaid(2L)).willReturn(false);
		given(emailDataQueryService.buildPaymentEmailContext(2L, event)).willReturn(Optional.of(paymentContext));

		consumer.handlePaymentCompleted(event);

		then(emailService).should(never()).sendBookingConfirmation(org.mockito.ArgumentMatchers.anyMap());
		then(emailService).should().sendPaymentConfirmation(paymentContext);
	}
}
