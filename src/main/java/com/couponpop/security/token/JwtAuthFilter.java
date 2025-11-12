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
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;

import java.io.IOException;
import java.util.List;

import static com.couponpop.security.constants.SecurityTemplates.SYSTEM_TOKEN_TYPE;
import static com.couponpop.security.constants.SecurityTemplates.USER_TOKEN_TYPE;


@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final List<PathPattern> whiteListPatterns;

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

        log.info("bearerToken-before: {}", request.getHeader(HttpHeaders.AUTHORIZATION));
        String bearerToken = jwtProvider.resolveToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        log.info("bearerToken-after: {}", bearerToken);

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
        PathContainer pathContainer = PathContainer.parsePath(uri);
        return whiteListPatterns.stream()
                .anyMatch(pattern -> pattern.matches(pathContainer));
    }

    private void setAuthentication(Claims claims) {
        String tokenType = claims.get("tokenType", String.class);
        AuthMember authMember;

        if (tokenType == null) {
            throw new MalformedJwtException("tokenType이 존재하지 않습니다.");
        }

        switch (tokenType) {
            case SYSTEM_TOKEN_TYPE -> {
                String systemName = claims.getSubject();
                authMember = AuthMember.ofSystem(systemName);
            }
            case USER_TOKEN_TYPE -> {
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                String memberType = claims.get("memberType", String.class);
                authMember = AuthMember.of(userId, username, memberType);
            }
            default -> throw new MalformedJwtException("알 수 없는 tokenType 입니다: " + tokenType);
        }

        Authentication authenticationToken = new JwtAuthenticationToken(authMember);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
