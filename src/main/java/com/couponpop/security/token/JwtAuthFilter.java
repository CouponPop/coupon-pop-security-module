package com.couponpop.security.token;


import com.couponpop.security.blacklist.service.TokenBlacklistService;
import com.couponpop.security.dto.AuthMember;
import com.couponpop.security.enums.MemberType;
import com.couponpop.utils.SecurityErrorResponseWriter;
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

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

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
                SecurityErrorResponseWriter.writeUnauthorizedResponse(response,
                        "INVALID_TOKEN", "유효하지 않은 토큰입니다. 다시 로그인하세요");
                return;
            }

            // 토큰 검증
            Claims claims = jwtProvider.validateToken(bearerToken);
            setAuthentication(claims);

        } catch (ExpiredJwtException e) {
            log.debug("[JwtFilter] 인증 실패: 만료된 토큰 - {}", e.getMessage());
            SecurityErrorResponseWriter.writeUnauthorizedResponse(response,
                    "EXPIRED_TOKEN", "만료된 토큰입니다. 다시 로그인하세요.");
            return;
        } catch (MalformedJwtException | SignatureException e) {
            log.debug("[JwtFilter] 인증 실패: 유효하지 않은 토큰 - {}", e.getMessage());
            SecurityErrorResponseWriter.writeUnauthorizedResponse(response,
                    "INVALID_TOKEN", "유효하지 않은 토큰입니다. 다시 로그인하세요");
            return;
        } catch (Exception e) {
            log.error("[JwtFilter] 인증 실패: 예상치 못한 오류 발생 - {}", e.getMessage(), e);
            SecurityErrorResponseWriter.writeInternalServerErrorResponse(response,
                    "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");
            return;
        }

        chain.doFilter(request, response);
    }

    private void setAuthentication(Claims claims) {
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        MemberType memberType = MemberType.valueOf(claims.get("memberType", String.class));

        AuthMember authMember = AuthMember.from(userId, username, memberType);
        Authentication authenticationToken = new JwtAuthenticationToken(authMember);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
