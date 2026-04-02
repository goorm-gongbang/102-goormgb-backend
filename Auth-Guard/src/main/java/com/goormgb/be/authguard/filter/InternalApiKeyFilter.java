package com.goormgb.be.authguard.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormgb.be.authguard.config.InternalApiKeyProperties;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.response.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

	private static final String HEADER_NAME = "X-Internal-Api-Key";
	private static final String INTERNAL_PATH_PREFIX = "/internal/";
	private static final String LOADTEST_PATH_PREFIX = "/loadtest/";

	private final InternalApiKeyProperties properties;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String servletPath = request.getServletPath();

		if (!servletPath.startsWith(INTERNAL_PATH_PREFIX)
			&& !servletPath.startsWith(LOADTEST_PATH_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		String apiKey = request.getHeader(HEADER_NAME);

		if (apiKey == null || !isEqual(apiKey, properties.getApiKey())) {
			log.warn("Internal API 인증 실패 - path: {}, remoteAddr: {}", servletPath, request.getRemoteAddr());
			sendErrorResponse(response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void sendErrorResponse(HttpServletResponse response) throws IOException {
		ErrorCode errorCode = ErrorCode.INVALID_INTERNAL_API_KEY;

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ErrorResponse.ErrorData errorData = ErrorResponse.ErrorData.of(
				errorCode.name(),
				errorCode.getMessage()
		);

		objectMapper.writeValue(response.getWriter(), errorData);
	}

	private boolean isEqual(String a, String b) {
		return MessageDigest.isEqual(
				a.getBytes(StandardCharsets.UTF_8),
				b.getBytes(StandardCharsets.UTF_8)
		);
	}
}
