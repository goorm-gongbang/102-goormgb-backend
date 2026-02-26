package com.goormgb.be.global.security.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class XUserIdAuthenticationFilter extends OncePerRequestFilter {

	private static final String HEADER_USER_ID = "X-User-Id";
	private static final String HEADER_USER_ROLE = "X-User-Role";

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		String userId = request.getHeader(HEADER_USER_ID);
		String userRole = request.getHeader(HEADER_USER_ROLE);

		if (userId != null && !userId.isBlank()) {
			SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
				userRole != null && !userRole.isBlank() ? userRole : "ROLE_USER"
			);

			UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(
					Long.valueOf(userId),
					null,
					List.of(authority)
				);

			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		filterChain.doFilter(request, response);
	}
}