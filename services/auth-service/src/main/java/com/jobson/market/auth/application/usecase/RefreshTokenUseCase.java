package com.jobson.market.auth.application.usecase;

import com.jobson.market.auth.application.service.RefreshTokenService;
import com.jobson.market.auth.domain.model.AuthTokens;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

public class RefreshTokenUseCase {

  private final RefreshTokenService refreshTokens;

  public RefreshTokenUseCase(RefreshTokenService refreshTokens) {
    this.refreshTokens = Objects.requireNonNull(refreshTokens);
  }

  @Transactional
  public LoginWithPasswordResult refresh(RefreshTokenCommand command) {
    AuthTokens tokens = refreshTokens.rotate(command.refreshToken());
    return new LoginWithPasswordResult(tokens.accessToken(), tokens.refreshToken());
  }
}
