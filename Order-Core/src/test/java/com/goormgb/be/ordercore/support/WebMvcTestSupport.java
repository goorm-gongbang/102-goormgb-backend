package com.goormgb.be.ordercore.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
import com.goormgb.be.global.jwt.filter.JwtAuthenticationFilter;

@ActiveProfiles("test")
public abstract class WebMvcTestSupport {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@MockitoBean
	protected JwtAuthenticationFilter jwtAuthenticationFilter;
}