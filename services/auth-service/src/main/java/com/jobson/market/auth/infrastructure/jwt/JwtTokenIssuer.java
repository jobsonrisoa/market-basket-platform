package com.jobson.market.auth.infrastructure.jwt;

import com.jobson.market.auth.application.port.AccessTokenIssuer;
import com.jobson.market.auth.application.port.TokenIssuer;
import com.jobson.market.auth.application.service.RefreshTokenService;
import com.jobson.market.auth.domain.model.AuthTokens;
import com.jobson.market.auth.domain.model.User;
import com.jobson.market.auth.infrastructure.config.AuthProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
class JwtTokenIssuer implements TokenIssuer, AccessTokenIssuer {

  private final JwtEncoder jwtEncoder;
  private final RefreshTokenService refreshTokenService;
  private final AuthProperties properties;
  private final Clock clock;

  JwtTokenIssuer(
      JwtEncoder jwtEncoder,
      RefreshTokenService refreshTokenService,
      AuthProperties properties,
      Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.refreshTokenService = refreshTokenService;
    this.properties = properties;
    this.clock = clock;
    this.refreshTokenService.setAccessTokenIssuer(this);
  }

  @Override
  public AuthTokens issue(User user) {
    RefreshTokenService.IssuedRawRefreshToken refreshToken = refreshTokenService.issueInitial(user);
    return new AuthTokens(issueAccessToken(user), refreshToken.rawToken());
  }

  @Override
  public String issueAccessToken(User user) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .id(UUID.randomUUID().toString())
            .issuer(properties.jwt().issuer())
            .audience(List.of(properties.jwt().audience()))
            .issuedAt(now)
            .expiresAt(now.plus(properties.jwt().accessTokenTtl()))
            .subject(user.id().toString())
            .claim("email", user.email().value())
            .claim("email_verified", user.emailVerified())
            .claim("scope", "auth:user")
            .build();
    JwsHeader headers =
        JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.jwt().keyId()).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
  }
}
