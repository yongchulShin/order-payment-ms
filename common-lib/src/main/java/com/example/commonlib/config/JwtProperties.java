package com.example.commonlib.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long accessTokenValidityInSeconds = 300; // 5 minutes
    private long refreshTokenValidityInSeconds = 2592000; // 30 days
} 