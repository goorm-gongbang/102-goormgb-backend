package com.goormgb.be.ordercore.email.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.goormgb.be.kafka.event.OrderCancelledEvent;
import com.goormgb.be.kafka.event.PaymentCompletedEvent;
import com.goormgb.be.ordercore.email.dto.EmailMessage;
import com.goormgb.be.ordercore.order.entity.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender mailSender;

	public void sendPaymentConfirmation(Order order, PaymentCompletedEvent event) {
		EmailMessage message = new EmailMessage(
				order.getOrdererEmail(),
				"[Playball] 결제 완료 안내",
				buildPaymentConfirmationBody(order, event)
		);
		send(message);
	}

	public void sendCancellationConfirmation(Order order, OrderCancelledEvent event) {
		EmailMessage message = new EmailMessage(
				order.getOrdererEmail(),
				"[Playball] 예매 취소 안내",
				buildCancellationBody(order, event)
		);
		send(message);
	}

	private void send(EmailMessage message) {
		try {
			SimpleMailMessage mail = new SimpleMailMessage();
			mail.setTo(message.to());
			mail.setSubject(message.subject());
			mail.setText(message.body());
			mail.setFrom("grgbdev@gmail.com");
			mailSender.send(mail);
			log.info("[Email] 발송 성공: to={}, subject={}", maskEmail(message.to()), message.subject());
		} catch (Exception e) {
			log.error("[Email] 발송 실패: to={}, subject={}, error={}",
					maskEmail(message.to()), message.subject(), e.getMessage(), e);
		}
	}

	private String buildPaymentConfirmationBody(Order order, PaymentCompletedEvent event) {
		return String.format("""
						안녕하세요, Playball입니다.
						
						결제가 완료되었습니다.
						
						주문번호: %d
						결제 금액: %,d원
						결제 방법: %s
						좌석 수: %d석
						
						즐거운 관람 되세요!
						""",
				order.getId(),
				event.getTotalAmount(),
				event.getPaymentMethod(),
				event.getMatchSeatIds().size()
		);
	}

	private String buildCancellationBody(Order order, OrderCancelledEvent event) {
		return String.format("""
						안녕하세요, Playball입니다.
						
						예매가 취소되었습니다.
						
						주문번호: %d
						취소 수수료: %,d원
						환불 금액: %,d원
						
						감사합니다.
						""",
				order.getId(),
				event.getCancellationFee(),
				event.getRefundedAmount()
		);
	}

	private String maskEmail(String email) {
		if (email == null || !email.contains("@"))
			return "***";
		String[] parts = email.split("@");
		String local = parts[0];
		String masked = local.length() <= 2
				? "*".repeat(local.length())
				: local.substring(0, 2) + "*".repeat(local.length() - 2);
		return masked + "@" + parts[1];
	}
}
