package com.jobson.market.auth.infrastructure.config;

import com.jobson.market.auth.application.port.crypto.PasswordHasher;
import com.jobson.market.auth.application.port.crypto.PasswordVerifier;
import com.jobson.market.auth.application.port.crypto.RefreshTokenCodec;
import com.jobson.market.auth.application.port.event.OutboxEventRepository;
import com.jobson.market.auth.application.port.identity.OAuthAccountRepository;
import com.jobson.market.auth.application.port.identity.PasswordCredentialRepository;
import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.application.port.token.RefreshTokenRepository;
import com.jobson.market.auth.application.port.token.TokenIssuer;
import com.jobson.market.auth.application.service.RefreshTokenService;
import com.jobson.market.auth.application.usecase.admin.AdminUserManagementUseCase;
import com.jobson.market.auth.application.usecase.authentication.GoogleLoginUseCase;
import com.jobson.market.auth.application.usecase.authentication.LoginWithPasswordUseCase;
import com.jobson.market.auth.application.usecase.authentication.LogoutUseCase;
import com.jobson.market.auth.application.usecase.authentication.RefreshTokenUseCase;
import com.jobson.market.auth.application.usecase.registration.RegisterUserUseCase;
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
  AdminUserManagementUseCase adminUserManagementUseCase(
      UserRepository users, OutboxEventRepository outbox) {
    return new AdminUserManagementUseCase(users, outbox);
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
