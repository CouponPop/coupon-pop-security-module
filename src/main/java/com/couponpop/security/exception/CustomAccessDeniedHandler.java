package com.couponpop.security.exception;

import com.couponpop.utils.SecurityErrorResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * 접근 거부 시 처리하는 핸들러
 * 서비스의 GlobalExceptionHandler와 독립적으로 동작
 */
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        SecurityErrorResponseWriter.writeForbiddenResponse(response,
                "ACCESS_DENIED", "접근 권한이 없습니다.");
    }
}