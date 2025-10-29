package com.couponpop.security.token;


import com.couponpop.security.blacklist.service.TokenBlacklistService;
import com.couponpop.security.dto.AuthMember;
import com.couponpop.security.utils.SecurityErrorResponseWriter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final List<String> whiteList;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {


        String requestURI = request.getRequestURI();

        if (isWhiteListed(requestURI)) {
            log.debug("[JwtFilter] 화이트리스트 대상 요청 - {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        String bearerToken = jwtProvider.resolveToken(request.getHeader(AUTHORIZATION_HEADER));

        try {
            // 토큰 존재 여부 확인
            if (!StringUtils.hasText(bearerToken)) {
                log.debug("[JwtFilter] 토큰이 존재하지 않는 요청");
                chain.doFilter(request, response);
                return;
            }

            // 블랙리스트 검증
            if (tokenBlacklistService.isBlacklisted(bearerToken)) {
                log.debug("[JwtFilter] 인증 실패: 블랙리스트에 등록된 토큰 - {}", bearerToken);
                SecurityErrorResponseWriter.writeInvalidTokenResponse(request, response);
                return;
            }

            // 토큰 검증
            Claims claims = jwtProvider.validateToken(bearerToken);
            setAuthentication(claims);

        } catch (ExpiredJwtException e) {
            log.debug("[JwtFilter] 인증 실패: 만료된 토큰 - {}", e.getMessage());
            SecurityErrorResponseWriter.writeExpiredTokenResponse(request, response);
            return;
        } catch (MalformedJwtException | SignatureException e) {
            log.debug("[JwtFilter] 인증 실패: 유효하지 않은 토큰 - {}", e.getMessage());
            SecurityErrorResponseWriter.writeInvalidTokenResponse(request, response);
            return;
        } catch (Exception e) {
            log.error("[JwtFilter] 인증 실패: 예상치 못한 오류 발생 - {}", e.getMessage(), e);
            SecurityErrorResponseWriter.writeInternalServerErrorResponse(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isWhiteListed(String uri) {
        return whiteList.stream().anyMatch(uri::startsWith);
    }

    private void setAuthentication(Claims claims) {
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String memberType = claims.get("memberType", String.class);

        AuthMember authMember = AuthMember.from(userId, username, memberType);
        Authentication authenticationToken = new JwtAuthenticationToken(authMember);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
