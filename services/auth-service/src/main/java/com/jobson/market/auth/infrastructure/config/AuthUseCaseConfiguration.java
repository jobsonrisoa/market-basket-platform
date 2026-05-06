package com.jobson.market.auth.infrastructure.config;

import com.jobson.market.auth.application.port.OAuthAccountRepository;
import com.jobson.market.auth.application.port.OutboxEventRepository;
import com.jobson.market.auth.application.port.PasswordCredentialRepository;
import com.jobson.market.auth.application.port.PasswordHasher;
import com.jobson.market.auth.application.port.PasswordVerifier;
import com.jobson.market.auth.application.port.RefreshTokenCodec;
import com.jobson.market.auth.application.port.RefreshTokenRepository;
import com.jobson.market.auth.application.port.TokenIssuer;
import com.jobson.market.auth.application.port.UserRepository;
import com.jobson.market.auth.application.service.RefreshTokenService;
import com.jobson.market.auth.application.usecase.GoogleLoginUseCase;
import com.jobson.market.auth.application.usecase.LoginWithPasswordUseCase;
import com.jobson.market.auth.application.usecase.LogoutUseCase;
import com.jobson.market.auth.application.usecase.RefreshTokenUseCase;
import com.jobson.market.auth.application.usecase.RegisterUserUseCase;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class AuthUseCaseConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  RefreshTokenService refreshTokenService(
      RefreshTokenRepository refreshTokens,
      RefreshTokenCodec refreshTokenCodec,
      UserRepository users,
      OutboxEventRepository outbox,
      Clock clock,
      AuthProperties properties) {
    return new RefreshTokenService(
        refreshTokens, refreshTokenCodec, users, outbox, clock, properties.refreshToken().ttl());
  }

  @Bean
  RegisterUserUseCase registerUserUseCase(
      UserRepository users,
      PasswordCredentialRepository credentials,
      PasswordHasher passwordHasher,
      OutboxEventRepository outbox) {
    return new RegisterUserUseCase(users, credentials, passwordHasher, outbox);
  }

  @Bean
  LoginWithPasswordUseCase loginWithPasswordUseCase(
      UserRepository users,
      PasswordCredentialRepository credentials,
      PasswordVerifier passwordVerifier,
      TokenIssuer tokenIssuer,
      OutboxEventRepository outbox) {
    return new LoginWithPasswordUseCase(users, credentials, passwordVerifier, tokenIssuer, outbox);
  }

  @Bean
  RefreshTokenUseCase refreshTokenUseCase(RefreshTokenService refreshTokenService) {
    return new RefreshTokenUseCase(refreshTokenService);
  }

  @Bean
  LogoutUseCase logoutUseCase(RefreshTokenService refreshTokenService) {
    return new LogoutUseCase(refreshTokenService);
  }

  @Bean
  GoogleLoginUseCase googleLoginUseCase(
      UserRepository users,
      OAuthAccountRepository oauthAccounts,
      TokenIssuer tokenIssuer,
      OutboxEventRepository outbox) {
    return new GoogleLoginUseCase(users, oauthAccounts, tokenIssuer, outbox);
  }
}
