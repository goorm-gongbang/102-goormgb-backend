package com.goormgb.be.ordercore.support;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.goormgb.be.global.environment.ErrorResponseStrategy;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.security.filter.XUserIdAuthenticationFilter;

import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
public abstract class WebMvcTestSupport {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@MockitoBean
	protected XUserIdAuthenticationFilter xUserIdAuthenticationFilter;

	@MockitoBean
	protected ErrorResponseStrategy errorResponseStrategy;

	@BeforeEach
	void stubErrorResponseStrategy() {
		lenient().when(errorResponseStrategy.resolveCode(any(ErrorCode.class)))
			.thenAnswer(invocation -> ((ErrorCode)invocation.getArgument(0)).name());
		lenient().when(errorResponseStrategy.resolveMessage(any(ErrorCode.class)))
			.thenAnswer(invocation -> ((ErrorCode)invocation.getArgument(0)).getMessage());
		lenient().when(errorResponseStrategy.resolveMessage(anyString(), any(HttpStatus.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}
}
