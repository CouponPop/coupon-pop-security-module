package com.couponpop.security.blacklist.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static com.couponpop.security.constants.SecurityTemplates.BLACKLIST_KEY_PREFIX;
import static com.couponpop.security.constants.SecurityTemplates.FORMATTER_YYYY_MM_DD_HH_MM_SS;

@Slf4j
public class RedisTokenBlacklistRepository implements TokenBlacklistRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTokenBlacklistRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static String tokenPreview(String token) {
        return token.substring(0, Math.min(10, token.length())) + "...";
    }

    @Override
    public void save(String token, long expirationMillis) {

        String key = BLACKLIST_KEY_PREFIX + token;
        long remainingMillis = expirationMillis - System.currentTimeMillis();

        if (remainingMillis <= 0) {
            log.debug("[Redis Blacklist Repository] 이미 만료된 토큰 블랙리스트 추가 시도 | token: {}", tokenPreview(token));
            return;
        }

        redisTemplate.opsForValue().set(key, String.valueOf(expirationMillis), remainingMillis, TimeUnit.MILLISECONDS);

        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(expirationMillis), ZoneId.of("Asia/Seoul"));
        log.debug("[Redis Blacklist Repository] 토큰 블랙리스트 추가 | token: {}, 만료 시각: {}, TTL: {}ms",
                tokenPreview(token),
                dateTime.format(FORMATTER_YYYY_MM_DD_HH_MM_SS),
                remainingMillis);
    }

    @Override
    public boolean exists(String token) {

        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("[Redis Blacklist Repository] Redis 조회 실패. 기본값 false 반환 | error: {}", e.getMessage());
            return false; // 기본값으로 false 반환 (가용성 우선)
        }
    }
}

