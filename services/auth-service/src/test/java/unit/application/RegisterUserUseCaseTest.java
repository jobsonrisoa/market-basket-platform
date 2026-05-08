package unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.auth.application.port.crypto.PasswordHasher;
import com.jobson.market.auth.application.port.event.OutboxEventRepository;
import com.jobson.market.auth.application.port.identity.PasswordCredentialRepository;
import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.application.usecase.DuplicateEmailException;
import com.jobson.market.auth.application.usecase.RegisterUserCommand;
import com.jobson.market.auth.application.usecase.RegisterUserResult;
import com.jobson.market.auth.application.usecase.RegisterUserUseCase;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.Password;
import com.jobson.market.auth.domain.model.Role;
import com.jobson.market.auth.domain.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisterUserUseCaseTest {

  @Test
  void shouldRegisterUserWithHashedPasswordAndOutboxEvent() {
    FakeUserRepository users = new FakeUserRepository();
    FakePasswordCredentialRepository credentials = new FakePasswordCredentialRepository();
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    RegisterUserUseCase useCase =
        new RegisterUserUseCase(users, credentials, new FakePasswordHasher(), outbox);

    RegisterUserResult result =
        useCase.register(new RegisterUserCommand("John@Example.COM", "Str0ng-password!"));

    assertEquals("john@example.com", result.email());
    assertTrue(result.roles().contains(Role.CUSTOMER));
    assertTrue(users.savedUser.isPresent());
    assertEquals(result.userId(), users.savedUser.orElseThrow().id());
    assertTrue(credentials.savedCredential.isPresent());
    assertEquals(result.userId(), credentials.savedCredential.orElseThrow().userId());
    assertEquals(
        "hashed::Str0ng-password!", credentials.savedCredential.orElseThrow().passwordHash());
    assertFalse(
        credentials.savedCredential.orElseThrow().passwordHash().equals("Str0ng-password!"));
    assertEquals(1, outbox.events.size());
    assertEquals("auth.user.registered.v1", outbox.events.get(0).eventType());
    assertEquals(result.userId().toString(), outbox.events.get(0).aggregateId());
  }

  @Test
  void shouldRejectAlreadyRegisteredEmail() {
    FakeUserRepository users = new FakeUserRepository();
    users.existingEmail = Optional.of(new Email("john@example.com"));
    FakePasswordCredentialRepository credentials = new FakePasswordCredentialRepository();
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    RegisterUserUseCase useCase =
        new RegisterUserUseCase(users, credentials, new FakePasswordHasher(), outbox);

    assertThrows(
        DuplicateEmailException.class,
        () -> useCase.register(new RegisterUserCommand("john@example.com", "Str0ng-password!")));

    assertTrue(credentials.savedCredential.isEmpty());
    assertTrue(outbox.events.isEmpty());
  }

  private static class FakeUserRepository implements UserRepository {
    private Optional<Email> existingEmail = Optional.empty();
    private Optional<User> savedUser = Optional.empty();

    @Override
    public boolean existsByEmail(Email email) {
      return existingEmail.filter(email::equals).isPresent();
    }

    @Override
    public Optional<User> findByEmail(Email email) {
      return Optional.empty();
    }

    @Override
    public Optional<User> findById(UUID id) {
      return Optional.empty();
    }

    @Override
    public User save(User user) {
      savedUser = Optional.of(user);
      return user;
    }
  }

  private static class FakePasswordCredentialRepository implements PasswordCredentialRepository {
    private Optional<SavedPasswordCredential> savedCredential = Optional.empty();

    @Override
    public void save(UUID userId, String passwordHash) {
      savedCredential = Optional.of(new SavedPasswordCredential(userId, passwordHash));
    }

    @Override
    public Optional<String> findPasswordHashByUserId(UUID userId) {
      return Optional.empty();
    }
  }

  private static class FakePasswordHasher implements PasswordHasher {

    @Override
    public String hash(Password password) {
      return "hashed::" + password.value();
    }
  }

  private static class FakeOutboxEventRepository implements OutboxEventRepository {
    private final List<OutboxEvent> events = new ArrayList<>();

    @Override
    public void save(OutboxEvent event) {
      events.add(event);
    }
  }

  private record SavedPasswordCredential(UUID userId, String passwordHash) {}
}
