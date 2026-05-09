package unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jobson.market.auth.application.port.crypto.PasswordVerifier;
import com.jobson.market.auth.application.port.event.OutboxEventRepository;
import com.jobson.market.auth.application.port.identity.PasswordCredentialRepository;
import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.application.port.token.TokenIssuer;
import com.jobson.market.auth.application.usecase.InvalidCredentialsException;
import com.jobson.market.auth.application.usecase.LoginWithPasswordCommand;
import com.jobson.market.auth.application.usecase.LoginWithPasswordResult;
import com.jobson.market.auth.application.usecase.LoginWithPasswordUseCase;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.AuthTokens;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.Password;
import com.jobson.market.auth.domain.model.User;
import com.jobson.market.auth.domain.model.UserStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoginWithPasswordUseCaseTest {

  @Test
  void shouldLoginWithValidPassword() {
    User user = new User(UUID.randomUUID(), new Email("john@example.com"), true, UserStatus.ACTIVE);
    FakeUserRepository users = new FakeUserRepository(Optional.of(user));
    FakePasswordCredentialRepository credentials =
        new FakePasswordCredentialRepository(Optional.of("hashed-password"));
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    LoginWithPasswordUseCase useCase =
        new LoginWithPasswordUseCase(
            users, credentials, new MatchingPasswordVerifier(), new FakeTokenIssuer(), outbox);

    LoginWithPasswordResult result =
        useCase.login(new LoginWithPasswordCommand("john@example.com", "Str0ng-password!"));

    assertEquals("access-token", result.accessToken());
    assertEquals("refresh-token", result.refreshToken());
    assertEquals(1, outbox.events.size());
    assertEquals("auth.session.login_succeeded.v1", outbox.events.get(0).eventType());
    assertEquals(user.id().toString(), outbox.events.get(0).aggregateId());
  }

  @Test
  void shouldRejectInvalidPassword() {
    User user = new User(UUID.randomUUID(), new Email("john@example.com"), true, UserStatus.ACTIVE);
    FakeUserRepository users = new FakeUserRepository(Optional.of(user));
    FakePasswordCredentialRepository credentials =
        new FakePasswordCredentialRepository(Optional.of("hashed-password"));
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    LoginWithPasswordUseCase useCase =
        new LoginWithPasswordUseCase(
            users, credentials, new RejectingPasswordVerifier(), new FakeTokenIssuer(), outbox);

    assertThrows(
        InvalidCredentialsException.class,
        () -> useCase.login(new LoginWithPasswordCommand("john@example.com", "Str0ng-password!")));

    assertEquals(1, outbox.events.size());
    assertEquals("auth.session.login_failed.v1", outbox.events.get(0).eventType());
  }

  @Test
  void shouldRejectUnknownEmailWithoutIssuingToken() {
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    FakeTokenIssuer tokenIssuer = new FakeTokenIssuer();
    LoginWithPasswordUseCase useCase =
        new LoginWithPasswordUseCase(
            new FakeUserRepository(Optional.empty()),
            new FakePasswordCredentialRepository(Optional.empty()),
            new MatchingPasswordVerifier(),
            tokenIssuer,
            outbox);

    assertThrows(
        InvalidCredentialsException.class,
        () -> useCase.login(new LoginWithPasswordCommand("john@example.com", "Str0ng-password!")));

    assertEquals(0, tokenIssuer.issuedTokens);
    assertEquals(1, outbox.events.size());
    assertEquals("auth.session.login_failed.v1", outbox.events.get(0).eventType());
  }

  @Test
  void shouldRejectSuspendedUserWithoutIssuingToken() {
    User user =
        new User(UUID.randomUUID(), new Email("john@example.com"), true, UserStatus.SUSPENDED);
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    FakeTokenIssuer tokenIssuer = new FakeTokenIssuer();
    LoginWithPasswordUseCase useCase =
        new LoginWithPasswordUseCase(
            new FakeUserRepository(Optional.of(user)),
            new FakePasswordCredentialRepository(Optional.of("hashed-password")),
            new MatchingPasswordVerifier(),
            tokenIssuer,
            outbox);

    assertThrows(
        InvalidCredentialsException.class,
        () -> useCase.login(new LoginWithPasswordCommand("john@example.com", "Str0ng-password!")));

    assertEquals(0, tokenIssuer.issuedTokens);
    assertEquals(1, outbox.events.size());
    assertEquals("auth.session.login_failed.v1", outbox.events.get(0).eventType());
  }

  private static class FakeUserRepository implements UserRepository {
    private final Optional<User> user;

    private FakeUserRepository(Optional<User> user) {
      this.user = user;
    }

    @Override
    public boolean existsByEmail(Email email) {
      return user.filter(found -> found.email().equals(email)).isPresent();
    }

    @Override
    public Optional<User> findByEmail(Email email) {
      return user.filter(found -> found.email().equals(email));
    }

    @Override
    public Optional<User> findById(UUID id) {
      return user.filter(found -> found.id().equals(id));
    }

    @Override
    public User save(User user) {
      return user;
    }
  }

  private static class FakePasswordCredentialRepository implements PasswordCredentialRepository {
    private final Optional<String> passwordHash;

    private FakePasswordCredentialRepository(Optional<String> passwordHash) {
      this.passwordHash = passwordHash;
    }

    @Override
    public void save(UUID userId, String passwordHash) {
      // Login tests only read existing credentials.
    }

    @Override
    public Optional<String> findPasswordHashByUserId(UUID userId) {
      return passwordHash;
    }
  }

  private static class MatchingPasswordVerifier implements PasswordVerifier {

    @Override
    public boolean matches(Password rawPassword, String passwordHash) {
      return true;
    }
  }

  private static class RejectingPasswordVerifier implements PasswordVerifier {

    @Override
    public boolean matches(Password rawPassword, String passwordHash) {
      return false;
    }
  }

  private static class FakeTokenIssuer implements TokenIssuer {
    private int issuedTokens;

    @Override
    public AuthTokens issue(User user) {
      issuedTokens++;
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
