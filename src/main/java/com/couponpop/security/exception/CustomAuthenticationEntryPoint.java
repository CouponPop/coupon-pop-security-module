package com.couponpop.security.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private HandlerExceptionResolver handlerExceptionResolver;

    public void setHandlerExceptionResolver(HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        if (handlerExceptionResolver != null) {
            handlerExceptionResolver.resolveException(request, response, null,
                    new GlobalException(AuthErrorCode.AUTHENTICATION_REQUIRED));
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Required");
        }
    }
}