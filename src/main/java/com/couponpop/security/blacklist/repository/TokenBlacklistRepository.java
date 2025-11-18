package com.couponpop.security.blacklist.repository;

public interface TokenBlacklistRepository {

    void save(String token, long expirationMillis);

    boolean exists(String token);
}
