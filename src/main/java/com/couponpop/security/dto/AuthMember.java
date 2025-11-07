package com.couponpop.security.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static com.couponpop.security.constants.SecurityTemplates.SYSTEM_TOKEN_TYPE;

public record AuthMember(
        Long id,
        String username,
        String memberType,
        GrantedAuthority role
) {
    public static AuthMember of(Long userId, String username, String memberTypeString) {

        // "ROLE_" 이라는 Prefix가 추가된 roleName 필드 사용
        GrantedAuthority role = new SimpleGrantedAuthority("ROLE_" + memberTypeString);
        return new AuthMember(userId, username, memberTypeString, role);
    }

    public static AuthMember ofSystem(String systemName) {
        GrantedAuthority role = new SimpleGrantedAuthority("ROLE_" + SYSTEM_TOKEN_TYPE);
        return new AuthMember(null, systemName, SYSTEM_TOKEN_TYPE, role);
    }
}