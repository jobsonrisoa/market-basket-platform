package unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.auth.application.port.event.OutboxEventRepository;
import com.jobson.market.auth.application.port.identity.OAuthAccountRepository;
import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.application.port.token.TokenIssuer;
import com.jobson.market.auth.application.usecase.authentication.GoogleLoginCommand;
import com.jobson.market.auth.application.usecase.authentication.GoogleLoginUseCase;
import com.jobson.market.auth.application.usecase.authentication.InvalidCredentialsException;
import com.jobson.market.auth.application.usecase.authentication.LoginWithPasswordResult;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.AccountProfile;
import com.jobson.market.auth.domain.model.AuthTokens;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.OAuthAccount;
import com.jobson.market.auth.domain.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GoogleLoginUseCaseTest {

  @Test
  void shouldLoginExistingLinkedGoogleAccount() {
    User user = User.register(new Email("john@example.com")).verifyEmail();
    FakeUserRepository users = new FakeUserRepository(user);
    FakeOAuthAccountRepository accounts = new FakeOAuthAccountRepository();
    accounts.save(OAuthAccount.linkGoogle(user.id(), "google-subject", user.email()));
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    GoogleLoginUseCase useCase =
        new GoogleLoginUseCase(users, accounts, new FakeTokenIssuer(), outbox);

    LoginWithPasswordResult result =
        useCase.login(new GoogleLoginCommand("google-subject", "john@example.com", true));

    assertEquals("access-token", result.accessToken());
    assertEquals("refresh-token", result.refreshToken());
    assertEquals(AccountProfile.CUSTOMER, result.accountProfile());
    assertEquals("auth.session.login_succeeded.v1", outbox.events.get(0).eventType());
  }

  @Test
  void shouldCreateVerifiedUserAndLinkGoogleAccount() {
    FakeUserRepository users = new FakeUserRepository();
    FakeOAuthAccountRepository accounts = new FakeOAuthAccountRepository();
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    GoogleLoginUseCase useCase =
        new GoogleLoginUseCase(users, accounts, new FakeTokenIssuer(), outbox);

    LoginWithPasswordResult result =
        useCase.login(new GoogleLoginCommand("google-subject", "john@example.com", true));

    assertEquals("access-token", result.accessToken());
    assertEquals(1, accounts.accountsBySubject.size());
    assertTrue(users.savedUser.emailVerified());
    assertEquals("auth.user.google_account_linked.v1", outbox.events.get(0).eventType());
    assertEquals("auth.session.login_succeeded.v1", outbox.events.get(1).eventType());
  }

  @Test
  void shouldRejectUnverifiedGoogleEmail() {
    GoogleLoginUseCase useCase =
        new GoogleLoginUseCase(
            new FakeUserRepository(),
            new FakeOAuthAccountRepository(),
            new FakeTokenIssuer(),
            new FakeOutboxEventRepository());
    GoogleLoginCommand command =
        new GoogleLoginCommand("google-subject", "john@example.com", false);

    assertThrows(InvalidCredentialsException.class, () -> useCase.login(command));
  }

  private static class FakeUserRepository implements UserRepository {
    private User savedUser;

    private FakeUserRepository(User savedUser) {
      this.savedUser = savedUser;
    }

    private FakeUserRepository() {}

    @Override
    public boolean existsByEmail(Email email) {
      return savedUser != null && savedUser.email().equals(email);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
      return existsByEmail(email) ? Optional.of(savedUser) : Optional.empty();
    }

    @Override
    public Optional<User> findById(UUID id) {
      return savedUser != null && savedUser.id().equals(id)
          ? Optional.of(savedUser)
          : Optional.empty();
    }

    @Override
    public User save(User user) {
      savedUser = user;
      return user;
    }
  }

  private static class FakeOAuthAccountRepository implements OAuthAccountRepository {
    private final Map<String, OAuthAccount> accountsBySubject = new HashMap<>();

    @Override
    public Optional<OAuthAccount> findByProviderAndSubject(
        String provider, String providerSubject) {
      return Optional.ofNullable(accountsBySubject.get(provider + ":" + providerSubject));
    }

    @Override
    public OAuthAccount save(OAuthAccount account) {
      accountsBySubject.put(account.provider() + ":" + account.providerSubject(), account);
      return account;
    }

    @Override
    public boolean existsByUserIdAndProvider(UUID userId, String provider) {
      return accountsBySubject.values().stream()
          .anyMatch(
              account -> account.userId().equals(userId) && account.provider().equals(provider));
    }
  }

  private static class FakeTokenIssuer implements TokenIssuer {
    @Override
    public AuthTokens issue(User user) {
      return new AuthTokens("access-token", "refresh-token");
    }
  }

  private static class FakeOutboxEventRepository implements OutboxEventRepository {
    private final List<OutboxEvent> events = new ArrayList<>();

    @Override
    public void save(OutboxEvent event) {
      events.add(event);
    }
  }
}
