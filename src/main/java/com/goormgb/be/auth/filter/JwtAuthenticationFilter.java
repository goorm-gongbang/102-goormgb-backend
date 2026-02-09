package com.goormgb.be.auth.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.goormgb.be.auth.enums.TokenType;
import com.goormgb.be.auth.provider.JwtTokenProvider;
import com.goormgb.be.auth.repository.AccessTokenBlacklistRepository;
import com.goormgb.be.global.exception.CustomException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    TokenType tokenType = jwtTokenProvider.getTokenTypeFromToken(token);

                    if (tokenType == TokenType.ACCESS) {
                        String jti = jwtTokenProvider.getJtiFromToken(token);

                        if (accessTokenBlacklistRepository.existsByJti(jti)) {
                            log.debug("Blacklisted token used - jti: {}", jti);
                        } else {
                            Long userId = jwtTokenProvider.getUserIdFromToken(token);
                            String authority = jwtTokenProvider.getAuthorityFromToken(token);

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userId,
                                            null,
                                            List.of(new SimpleGrantedAuthority(authority))
                                    );

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("Set authentication for user: {}", userId);
                        }
                    }
                }
            } catch (CustomException e) {
                log.debug("JWT validation failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}