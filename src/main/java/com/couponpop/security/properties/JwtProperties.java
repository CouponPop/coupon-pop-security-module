package com.couponpop.security.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Setter
@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private Secret secret;

    @Setter
    @Getter
    public static class Secret {
        private String key;
        private List<String> whiteList;
    }
}