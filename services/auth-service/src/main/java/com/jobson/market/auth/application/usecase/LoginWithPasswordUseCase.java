package com.jobson.market.auth.application.usecase;

import com.jobson.market.auth.application.port.OutboxEventRepository;
import com.jobson.market.auth.application.port.PasswordCredentialRepository;
import com.jobson.market.auth.application.port.PasswordVerifier;
import com.jobson.market.auth.application.port.TokenIssuer;
import com.jobson.market.auth.application.port.UserRepository;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.AuthTokens;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.Password;
import com.jobson.market.auth.domain.model.User;
import java.util.Objects;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public class LoginWithPasswordUseCase {

  private final UserRepository users;
  private final PasswordCredentialRepository credentials;
  private final PasswordVerifier passwordVerifier;
  private final TokenIssuer tokenIssuer;
  private final OutboxEventRepository outbox;

  public LoginWithPasswordUseCase(
      UserRepository users,
      PasswordCredentialRepository credentials,
      PasswordVerifier passwordVerifier,
      TokenIssuer tokenIssuer,
      OutboxEventRepository outbox) {
    this.users = Objects.requireNonNull(users);
    this.credentials = Objects.requireNonNull(credentials);
    this.passwordVerifier = Objects.requireNonNull(passwordVerifier);
    this.tokenIssuer = Objects.requireNonNull(tokenIssuer);
    this.outbox = Objects.requireNonNull(outbox);
  }

  @Transactional
  public LoginWithPasswordResult login(LoginWithPasswordCommand command) {
    Email email = new Email(command.email());
    Password password = new Password(command.password());
    Optional<User> user = users.findByEmail(email);
    if (user.isEmpty()) {
      outbox.save(OutboxEvent.loginFailed(email));
      throw new InvalidCredentialsException();
    }

    User foundUser = user.orElseThrow();
    String passwordHash =
        credentials
            .findPasswordHashByUserId(foundUser.id())
            .orElseThrow(InvalidCredentialsException::new);
    if (!passwordVerifier.matches(password, passwordHash)) {
      outbox.save(OutboxEvent.loginFailed(email));
      throw new InvalidCredentialsException();
    }

    AuthTokens tokens = tokenIssuer.issue(foundUser);
    outbox.save(OutboxEvent.loginSucceeded(foundUser));
    return new LoginWithPasswordResult(tokens.accessToken(), tokens.refreshToken());
  }
}
