package com.goormgb.be.logging;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class RequestLoggingFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest)request;

		try {
			// MDC에 HTTP 헤더 정보 추가
			MDC.put("request_ip", getClientIP(httpRequest));
			MDC.put("user_agent", httpRequest.getHeader("User-Agent"));
			MDC.put("referer", httpRequest.getHeader("Referer"));

			var session = httpRequest.getSession(false);
			MDC.put("session_id", session != null ? session.getId() : "");

			// 결제 요청인지 확인하여 log_category 설정
			String uri = httpRequest.getRequestURI();
			// TODO: /api/v1/payments/** 처럼 변경
			if (uri.contains("/payment")) {
				MDC.put("log_category", "PAYMENT");
			} else {
				MDC.put("log_category", "INFO");
			}

			chain.doFilter(request, response);
		} finally {
			// MDC 정리 (메모리 누수 방지)
			MDC.clear();
		}
	}

	private String getClientIP(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}
}