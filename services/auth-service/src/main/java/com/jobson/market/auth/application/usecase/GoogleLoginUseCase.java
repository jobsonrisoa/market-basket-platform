package com.jobson.market.auth.application.usecase;

import com.jobson.market.auth.application.port.OAuthAccountRepository;
import com.jobson.market.auth.application.port.OutboxEventRepository;
import com.jobson.market.auth.application.port.TokenIssuer;
import com.jobson.market.auth.application.port.UserRepository;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.AuthTokens;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.OAuthAccount;
import com.jobson.market.auth.domain.model.User;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

public class GoogleLoginUseCase {

  private final UserRepository users;
  private final OAuthAccountRepository oauthAccounts;
  private final TokenIssuer tokenIssuer;
  private final OutboxEventRepository outbox;

  public GoogleLoginUseCase(
      UserRepository users,
      OAuthAccountRepository oauthAccounts,
      TokenIssuer tokenIssuer,
      OutboxEventRepository outbox) {
    this.users = Objects.requireNonNull(users);
    this.oauthAccounts = Objects.requireNonNull(oauthAccounts);
    this.tokenIssuer = Objects.requireNonNull(tokenIssuer);
    this.outbox = Objects.requireNonNull(outbox);
  }

  @Transactional
  public LoginWithPasswordResult login(GoogleLoginCommand command) {
    if (!command.emailVerified()) {
      throw new InvalidCredentialsException();
    }
    Email email = new Email(command.email());
    User user =
        oauthAccounts
            .findByProviderAndSubject("google", command.subject())
            .flatMap(account -> users.findById(account.userId()))
            .orElseGet(() -> linkGoogleAccount(command.subject(), email));

    AuthTokens tokens = tokenIssuer.issue(user);
    outbox.save(OutboxEvent.loginSucceeded(user));
    return new LoginWithPasswordResult(tokens.accessToken(), tokens.refreshToken());
  }

  private User linkGoogleAccount(String subject, Email email) {
    User user =
        users.findByEmail(email).orElseGet(() -> users.save(User.register(email).verifyEmail()));
    if (!oauthAccounts.existsByUserIdAndProvider(user.id(), "google")) {
      oauthAccounts.save(OAuthAccount.linkGoogle(user.id(), subject, email));
      outbox.save(OutboxEvent.googleAccountLinked(user));
    }
    return user;
  }
}
