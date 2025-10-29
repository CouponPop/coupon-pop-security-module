package com.couponpop.security.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Security 관련 HTTP 에러 응답을 JSON 형식으로 작성하는 유틸리티 클래스
 *
 * <p>JwtAuthFilter, CustomAccessDeniedHandler, CustomAuthenticationEntryPoint 등에서
 * 공통으로 사용하는 에러 응답 로직을 통합 관리합니다.</p>
 *
 * <p>서비스의 GlobalExceptionHandler와 독립적으로 동작하여 충돌을 방지합니다.</p>
 */
public final class SecurityErrorResponseWriter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 유틸리티 클래스이므로 인스턴스화 방지
    private SecurityErrorResponseWriter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * HTTP 에러 응답을 JSON 형식으로 작성합니다.
     *
     * @param response  HTTP 응답 객체
     * @param status    HTTP 상태 코드 (예: 401, 403, 500)
     * @param errorCode 에러 코드 (예: "INVALID_TOKEN", "ACCESS_DENIED")
     * @param message   에러 메시지
     * @throws IOException 응답 작성 중 IO 예외 발생 시
     */
    public static void writeErrorResponse(HttpServletResponse response,
                                          int status,
                                          String errorCode,
                                          String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 401 Unauthorized 응답을 작성합니다.
     *
     * @param response  HTTP 응답 객체
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     * @throws IOException 응답 작성 중 IO 예외 발생 시
     */
    public static void writeUnauthorizedResponse(HttpServletResponse response,
                                                 String errorCode,
                                                 String message) throws IOException {
        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, errorCode, message);
    }

    /**
     * 403 Forbidden 응답을 작성합니다.
     *
     * @param response  HTTP 응답 객체
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     * @throws IOException 응답 작성 중 IO 예외 발생 시
     */
    public static void writeForbiddenResponse(HttpServletResponse response,
                                              String errorCode,
                                              String message) throws IOException {
        writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, errorCode, message);
    }

    /**
     * 500 Internal Server Error 응답을 작성합니다.
     *
     * @param response  HTTP 응답 객체
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     * @throws IOException 응답 작성 중 IO 예외 발생 시
     */
    public static void writeInternalServerErrorResponse(HttpServletResponse response,
                                                        String errorCode,
                                                        String message) throws IOException {
        writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorCode, message);
    }
}

