package com.goormgb.be.authguard.filter;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormgb.be.authguard.config.InternalApiKeyProperties;

class InternalApiKeyFilterTest {

	private static final String VALID_API_KEY = "test-internal-api-key";
	private static final String HEADER_NAME = "X-Internal-Api-Key";

	private InternalApiKeyFilter filter;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		InternalApiKeyProperties properties = new InternalApiKeyProperties(VALID_API_KEY);
		filter = new InternalApiKeyFilter(properties, objectMapper);
	}

	private MockHttpServletRequest createRequest(String method, String path) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setServletPath(path);
		return request;
	}

	@Test
	@DisplayName("유효한 API Key로 /internal/** 요청 시 정상 통과")
	void 유효한_API_Key_정상_통과() throws Exception {
		// given
		MockHttpServletRequest request = createRequest("POST", "/internal/users/1/block");
		request.addHeader(HEADER_NAME, VALID_API_KEY);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		// when
		filter.doFilterInternal(request, response, filterChain);

		// then
		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(filterChain.getRequest()).isNotNull();
	}

	@Test
	@DisplayName("API Key 헤더 누락 시 401 응답")
	void API_Key_헤더_누락_401() throws Exception {
		// given
		MockHttpServletRequest request = createRequest("POST", "/internal/users/1/block");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		// when
		filter.doFilterInternal(request, response, filterChain);

		// then
		assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(response.getContentAsString()).contains("INVALID_INTERNAL_API_KEY");
		assertThat(filterChain.getRequest()).isNull();
	}

	@Test
	@DisplayName("잘못된 API Key로 요청 시 401 응답")
	void 잘못된_API_Key_401() throws Exception {
		// given
		MockHttpServletRequest request = createRequest("POST", "/internal/users/1/unblock");
		request.addHeader(HEADER_NAME, "wrong-api-key");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		// when
		filter.doFilterInternal(request, response, filterChain);

		// then
		assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(response.getContentAsString()).contains("INVALID_INTERNAL_API_KEY");
		assertThat(filterChain.getRequest()).isNull();
	}

	@Test
	@DisplayName("/internal/** 이 아닌 경로는 필터를 통과")
	void 일반_경로_필터_통과() throws Exception {
		// given
		MockHttpServletRequest request = createRequest("POST", "/token/refresh");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		// when
		filter.doFilterInternal(request, response, filterChain);

		// then
		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(filterChain.getRequest()).isNotNull();
	}
}
