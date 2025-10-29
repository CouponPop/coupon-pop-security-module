package com.couponpop.security.exception;

import com.couponpop.utils.SecurityErrorResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;


/**
 * 인증되지 않은 사용자의 접근 시 처리하는 진입점
 * 서비스의 GlobalExceptionHandler와 독립적으로 동작
 */
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        SecurityErrorResponseWriter.writeUnauthorizedResponse(response,
                "AUTHENTICATION_REQUIRED", "인증이 필요합니다.");
    }
}