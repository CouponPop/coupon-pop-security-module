package com.couponpop.security.config;

import com.couponpop.security.blacklist.repository.RedisTokenBlacklistRepository;
import com.couponpop.security.blacklist.repository.TokenBlacklistRepository;
import com.couponpop.security.blacklist.service.TokenBlacklistService;
import com.couponpop.security.exception.CustomAccessDeniedHandler;
import com.couponpop.security.exception.CustomAuthenticationEntryPoint;
import com.couponpop.security.properties.JwtProperties;
import com.couponpop.security.token.JwtAuthFilter;
import com.couponpop.security.token.JwtProvider;
import com.couponpop.security.token.SystemTokenProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

@AutoConfiguration
@ConfigurationPropertiesScan("com.couponpop.security.properties")
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityBeansConfig {

    /**
     * JWT 토큰 생성 및 검증을 담당하는 Provider를 제공합니다.
     * 서비스에서 JwtProvider 빈을 정의하면 해당 빈이 우선 사용됩니다.
     */
    @Bean
    @ConditionalOnMissingBean(JwtProvider.class)
    public JwtProvider jwtProvider() {
        return new JwtProvider();
    }

    /**
     * JWT 인증 필터를 제공합니다.
     * 서비스에서 JwtAuthFilter 빈을 정의하면 해당 빈이 우선 사용됩니다.
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthFilter.class)
    public JwtAuthFilter jwtAuthFilter(JwtProvider jwtProvider,
                                       TokenBlacklistService tokenBlacklistService, JwtProperties jwtProperties) {

        List<String> whiteList = jwtProperties.getSecret().getWhiteList();
        PathPatternParser pathPatternParser = new PathPatternParser();
        List<PathPattern> whiteListPatterns = whiteList.stream()
                .map(pathPatternParser::parse)
                .toList();
        return new JwtAuthFilter(jwtProvider, tokenBlacklistService, whiteListPatterns);
    }

    /**
     * 토큰 블랙리스트 저장소를 제공합니다.
     * 기본 구현은 Redis 방식이며, 다른 구현체로 재정의 가능합니다.
     *
     * @return Redis 방식의 토큰 블랙리스트 저장소
     */
    @Bean
    @ConditionalOnMissingBean(TokenBlacklistRepository.class)
    public TokenBlacklistRepository tokenBlacklistRepository(StringRedisTemplate stringRedisTemplate) {
        return new RedisTokenBlacklistRepository(stringRedisTemplate);
    }

    /**
     * 토큰 블랙리스트 서비스를 제공합니다.
     * 서비스에서 TokenBlacklistService 빈을 정의하면 해당 빈이 우선 사용됩니다.
     */
    @Bean
    @ConditionalOnMissingBean(TokenBlacklistService.class)
    public TokenBlacklistService tokenBlacklistService(TokenBlacklistRepository tokenBlacklistRepository) {
        return new TokenBlacklistService(tokenBlacklistRepository);
    }

    /**
     * 접근 거부 시 처리를 담당하는 핸들러를 제공합니다.
     * 서비스에서 CustomAccessDeniedHandler 빈을 정의하면 해당 빈이 우선 사용됩니다.
     */
    @Bean
    @ConditionalOnMissingBean(CustomAccessDeniedHandler.class)
    public CustomAccessDeniedHandler customAccessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }

    /**
     * 인증되지 않은 사용자의 접근 시 처리를 담당하는 진입점을 제공합니다.
     * 서비스에서 CustomAuthenticationEntryPoint 빈을 정의하면 해당 빈이 우선 사용됩니다.
     */
    @Bean
    @ConditionalOnMissingBean(CustomAuthenticationEntryPoint.class)
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint();
    }

    @Bean
    @ConditionalOnMissingBean(SystemTokenProvider.class)
    public SystemTokenProvider systemTokenProvider(JwtProvider jwtProvider) {
        return new SystemTokenProvider(jwtProvider);
    }
}
