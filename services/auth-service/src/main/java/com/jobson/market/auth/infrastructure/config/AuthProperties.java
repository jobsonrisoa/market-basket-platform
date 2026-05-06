package com.jobson.market.auth.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(Jwt jwt, RefreshToken refreshToken) {

  public record Jwt(String issuer, String audience, Duration accessTokenTtl, String keyId) {}

  public record RefreshToken(Duration ttl) {}
}
