package com.jobson.market.auth.application.usecase;

import com.jobson.market.auth.application.service.RefreshTokenService;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class LogoutUseCase {

  private final RefreshTokenService refreshTokens;

  public LogoutUseCase(RefreshTokenService refreshTokens) {
    this.refreshTokens = Objects.requireNonNull(refreshTokens);
  }

  @Transactional
  public void logout(LogoutCommand command) {
    refreshTokens.revokeSession(command.refreshToken());
  }

  @Transactional
  public void logoutAll(UUID userId) {
    refreshTokens.revokeAll(userId);
  }
}
