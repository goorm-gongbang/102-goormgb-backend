package com.goormgb.be.ordercore.email.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.goormgb.be.ordercore.email.config.FrontendUrlProvider;
import com.goormgb.be.ordercore.email.dto.EmailMessage;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final FrontendUrlProvider frontendUrlProvider;

	@Value("${app.mail.from:no-reply@playball.one}")
	private String mailFrom;

	@Value("${app.mail.from-name:Playball}")
	private String mailFromName;

	private static final ClassPathResource LOGO_RESOURCE =
			new ClassPathResource("static/images/playball-logo.png");
	private static final ClassPathResource MAIL_RESOURCE =
			new ClassPathResource("static/images/mail.png");

	public void sendPaymentConfirmation(Map<String, Object> emailContext) {
		String to = extractEmail(emailContext);
		Context context = buildContext(emailContext);
		String htmlBody = templateEngine.process("email/payment-confirmation", context);
		send(new EmailMessage(to, "[Playball] 결제 완료 안내", htmlBody));
	}

	public void sendBookingConfirmation(Map<String, Object> emailContext) {
		String to = extractEmail(emailContext);
		Context context = buildContext(emailContext);
		String htmlBody = templateEngine.process("email/booking-confirmation", context);
		send(new EmailMessage(to, "[Playball] 티켓 예매 완료 안내", htmlBody));
	}

	public void sendCancellationConfirmation(Map<String, Object> emailContext) {
		String to = extractEmail(emailContext);
		Context context = buildContext(emailContext);
		String htmlBody = templateEngine.process("email/cancellation-confirmation", context);
		send(new EmailMessage(to, "[Playball] 티켓 예매 취소 안내", htmlBody));
	}

	public void sendForcedCancellation(Map<String, Object> emailContext) {
		String to = extractEmail(emailContext);
		Context context = buildContext(emailContext);
		String htmlBody = templateEngine.process("email/forced-cancellation", context);
		send(new EmailMessage(to, "[Playball] 악성 유저 의심으로 인한 티켓 예매 보류 안내", htmlBody));
	}

	private Context buildContext(Map<String, Object> emailContext) {
		Context context = new Context();
		emailContext.forEach(context::setVariable);
		context.setVariable("myTicketsUrl", frontendUrlProvider.getBaseUrl() + "/my/reservations");
		return context;
	}

	private String extractEmail(Map<String, Object> emailContext) {
		String to = (String)emailContext.get("toEmail");
		if (to == null || to.isBlank()) {
			to = (String)emailContext.get("ordererEmail");
		}
		return to != null ? to : "";
	}

	private void send(EmailMessage message) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			helper.setTo(message.to());
			helper.setSubject(message.subject());
			helper.setText(message.body(), true);
			helper.setFrom(mailFrom, mailFromName);

			if (LOGO_RESOURCE.exists()) {
				helper.addInline("playball-logo", LOGO_RESOURCE, "image/png");
			}
			if (MAIL_RESOURCE.exists()) {
				helper.addInline("mailIcon", MAIL_RESOURCE, "image/png");
			}

			mailSender.send(mimeMessage);
			log.info("[Email] 발송 성공: to={}, subject={}", maskEmail(message.to()), message.subject());
		} catch (Exception e) {
			log.error("[Email] 발송 실패: to={}, subject={}, error={}",
					maskEmail(message.to()), message.subject(), e.getMessage(), e);
		}
	}

	private String maskEmail(String email) {
		if (email == null || !email.contains("@")) {
			return "***";
		}
		String[] parts = email.split("@");
		String local = parts[0];
		String masked = local.length() <= 2
				? "*".repeat(local.length())
				: local.substring(0, 2) + "*".repeat(local.length() - 2);
		return masked + "@" + parts[1];
	}
}
