package com.couponpop.security.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityTemplates {

    public static final DateTimeFormatter FORMATTER_YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final long ACCESS_TOKEN_EXPIRATION = 60 * 60 * 1000L; // 1시간
    public static final long SYSTEM_TOKEN_EXPIRATION = 5 * 60 * 1000L; // 5분
    public static final String USER_TOKEN_TYPE = "USER";
    public static final String SYSTEM_TOKEN_TYPE = "SYSTEM";
    public static final String BEARER_TOKEN_PREFIX = "Bearer ";
    public static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
}
