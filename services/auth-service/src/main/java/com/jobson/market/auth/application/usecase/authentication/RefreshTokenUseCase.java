package com.jobson.market.auth.application.usecase.authentication;

import com.jobson.market.auth.application.port.crypto.RefreshTokenCodec;
import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.application.port.token.RefreshTokenRepository;
import com.jobson.market.auth.application.service.RefreshTokenService;
import com.jobson.market.auth.domain.model.AuthTokens;
import com.jobson.market.auth.domain.model.User;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

public class RefreshTokenUseCase {

  private final RefreshTokenService refreshTokens;
  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenCodec refreshTokenCodec;
  private final UserRepository users;

  public RefreshTokenUseCase(
      RefreshTokenService refreshTokens,
      RefreshTokenRepository refreshTokenRepository,
      RefreshTokenCodec refreshTokenCodec,
      UserRepository users) {
    this.refreshTokens = Objects.requireNonNull(refreshTokens);
    this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository);
    this.refreshTokenCodec = Objects.requireNonNull(refreshTokenCodec);
    this.users = Objects.requireNonNull(users);
  }

  @Transactional
  public LoginWithPasswordResult refresh(RefreshTokenCommand command) {
    RefreshTokenRepository.StoredRefreshToken current =
        refreshTokenRepository
            .findByTokenHash(refreshTokenCodec.hash(command.refreshToken()))
            .orElseThrow(InvalidRefreshTokenException::new);
    User user = users.findById(current.userId()).orElseThrow(InvalidRefreshTokenException::new);
    AuthTokens tokens = refreshTokens.rotate(command.refreshToken());
    return LoginWithPasswordResult.from(tokens, user);
  }
}
