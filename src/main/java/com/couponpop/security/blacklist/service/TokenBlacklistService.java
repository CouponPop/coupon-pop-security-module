package com.couponpop.security.blacklist.service;

import com.couponpop.security.blacklist.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;

    public void blacklistToken(String token, long expirationMillis) {

        tokenBlacklistRepository.save(token, expirationMillis);
    }

    public boolean isBlacklisted(String token) {

        return tokenBlacklistRepository.exists(token);
    }
}
