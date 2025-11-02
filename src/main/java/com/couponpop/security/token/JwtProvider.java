package com.couponpop.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static com.couponpop.security.constants.SecurityTemplates.ACCESS_TOKEN_EXPIRATION;
import static com.couponpop.security.constants.SecurityTemplates.BEARER_TOKEN_PREFIX;

@Slf4j(topic = "JwtUtil")
public class JwtProvider {

    @Value("${jwt.secret.key}")
    private String base64SecretKey;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(base64SecretKey);
        this.secretKey = Keys.hmacShaKeyFor(bytes); // 미리 암호화
    }

    public String createAccessToken(Long userId, String username, String memberType) {

        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("memberType", memberType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String resolveToken(String authorizationHeader) {

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_TOKEN_PREFIX)) {
            return authorizationHeader.substring(BEARER_TOKEN_PREFIX.length());
        }
        return null;
    }

    public long getExpirationMillis(String token) {
        return validateToken(token).getExpiration().getTime();
    }
}
