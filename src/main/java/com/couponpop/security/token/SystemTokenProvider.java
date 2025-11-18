package com.couponpop.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 시스템 간 통신에 사용되는 JWT 토큰을 제공하는 클래스
 *
 * <ul>
 *     <li>토큰을 캐싱하여 재사용함으로써 불필요한 토큰 발급을 줄임</li>
 *     <li>토큰 만료 시간 전에 자동으로 갱신</li>
 *     <li>
 *         {@code compareAndSet} + {@code AtomicReference}
 *         <ul>
 *             <li>여러 쓰레드가 동시에 들어와도 됨</li>
 *             <li>중복 발급은 생길 수 있지만 상태(ref)는 항상 정상</li>
 *             <li>{@code synchronized}와 달리 락을 안 걸기 때문에 블로킹 없음, 성능/확장성 좋음</li>
 *         </ul>
 *     </li>
 * </ul>
 */
@RequiredArgsConstructor
public class SystemTokenProvider {

    // 실제 만료 시간보다 조금 여유 있게 갱신하기 위한 여유 시간 (10초)
    private static final long EXPIRE_SKEW_MILLIS = 10_000L;

    // 캐시된 토큰 상태 (토큰 문자열 + 만료시각)
    private final AtomicReference<TokenCache> cacheRef = new AtomicReference<>();
    private final JwtProvider jwtProvider;

    @Value("${spring.application.name}")
    private String systemName;

    public String getToken() {
        long now = System.currentTimeMillis();

        // 현재 캐시된 토큰이 유효하면 그대로 사용
        TokenCache current = cacheRef.get();
        if (current != null && current.isValid(now)) {
            return current.token();
        }

        // 새 토큰 발급
        String newToken = jwtProvider.createSystemToken(systemName);
        long expMillis = jwtProvider.getExpirationMillis(newToken);
        long expiresAt = expMillis - EXPIRE_SKEW_MILLIS;

        TokenCache newCache = new TokenCache(newToken, expiresAt);

        // CAS로 교체 (여러 쓰레드가 동시에 들어와도 일관성 문제 없음)
        cacheRef.compareAndSet(current, newCache);

        return newCache.token();
    }

    private record TokenCache(String token, long expiresAt) {
        boolean isValid(long now) {
            return now < expiresAt;
        }
    }
}
