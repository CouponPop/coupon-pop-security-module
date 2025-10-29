package com.couponpop.security.utils;

import com.couponpop.security.exception.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Security 관련 HTTP 에러 응답을 JSON 형식으로 작성하는 유틸리티 클래스
 * 서비스의 GlobalExceptionHandler와 독립적으로 동작하여 충돌을 방지합니다.
 *
 */
public final class SecurityErrorResponseWriter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private SecurityErrorResponseWriter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * AuthErrorCode를 사용하여 에러 응답을 작성합니다.
     *
     * @param request   HTTP 요청 객체 (URL 추출용)
     * @param response  HTTP 응답 객체
     * @param errorCode AuthErrorCode enum
     * @throws IOException 응답 작성 중 IO 예외 발생 시
     */
    public static void writeErrorResponse(HttpServletRequest request,
                                          HttpServletResponse response,
                                          AuthErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", errorCode.getHttpStatus().value() + " " + errorCode.getHttpStatus().name());
        error.put("code", errorCode.name());
        error.put("message", errorCode.getMessage());
        error.put("requestUrl", request.getRequestURI());
        error.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("error", error);

        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }

    /**
     * 401 Unauthorized 응답을 작성합니다. (인증 필요)
     */
    public static void writeAuthenticationRequiredResponse(HttpServletRequest request,
                                                           HttpServletResponse response) throws IOException {
        writeErrorResponse(request, response, AuthErrorCode.AUTHENTICATION_REQUIRED);
    }

    /**
     * 401 Unauthorized 응답을 작성합니다. (유효하지 않은 토큰)
     */
    public static void writeInvalidTokenResponse(HttpServletRequest request,
                                                 HttpServletResponse response) throws IOException {
        writeErrorResponse(request, response, AuthErrorCode.INVALID_TOKEN);
    }

    /**
     * 401 Unauthorized 응답을 작성합니다. (만료된 토큰)
     */
    public static void writeExpiredTokenResponse(HttpServletRequest request,
                                                 HttpServletResponse response) throws IOException {
        writeErrorResponse(request, response, AuthErrorCode.EXPIRED_TOKEN);
    }

    /**
     * 403 Forbidden 응답을 작성합니다. (접근 권한 없음)
     */
    public static void writeAccessDeniedResponse(HttpServletRequest request,
                                                 HttpServletResponse response) throws IOException {
        writeErrorResponse(request, response, AuthErrorCode.ACCESS_DENIED);
    }

    /**
     * 500 Internal Server Error 응답을 작성합니다.
     */
    public static void writeInternalServerErrorResponse(HttpServletRequest request,
                                                        HttpServletResponse response) throws IOException {
        writeErrorResponse(request, response, AuthErrorCode.INTERNAL_SERVER_ERROR);
    }
}
