package com.couponpop.security.config;

import com.couponpop.security.blacklist.repository.InMemoryTokenBlacklistRepository;
import com.couponpop.security.blacklist.repository.TokenBlacklistRepository;
import com.couponpop.security.blacklist.service.TokenBlacklistService;
import com.couponpop.security.exception.CustomAccessDeniedHandler;
import com.couponpop.security.exception.CustomAuthenticationEntryPoint;
import com.couponpop.security.properties.JwtProperties;
import com.couponpop.security.token.JwtAuthFilter;
import com.couponpop.security.token.JwtProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
                                       TokenBlacklistService tokenBlacklistService) {
        return new JwtAuthFilter(jwtProvider, tokenBlacklistService);
    }

    /**
     * 토큰 블랙리스트 저장소를 제공합니다.
     * 기본 구현은 InMemory 방식이며, Redis 등 다른 구현체로 재정의 가능합니다.
     *
     * @return InMemory 방식의 토큰 블랙리스트 저장소
     */
    @Bean
    @ConditionalOnMissingBean(TokenBlacklistRepository.class)
    public TokenBlacklistRepository tokenBlacklistRepository() {
        return new InMemoryTokenBlacklistRepository();
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
}
