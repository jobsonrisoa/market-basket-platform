package com.jobson.market.auth.application.usecase;

import com.jobson.market.auth.application.port.OutboxEventRepository;
import com.jobson.market.auth.application.port.PasswordCredentialRepository;
import com.jobson.market.auth.application.port.PasswordHasher;
import com.jobson.market.auth.application.port.UserRepository;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.Password;
import com.jobson.market.auth.domain.model.User;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

public class RegisterUserUseCase {

  private final UserRepository users;
  private final PasswordCredentialRepository credentials;
  private final PasswordHasher passwordHasher;
  private final OutboxEventRepository outbox;

  public RegisterUserUseCase(
      UserRepository users,
      PasswordCredentialRepository credentials,
      PasswordHasher passwordHasher,
      OutboxEventRepository outbox) {
    this.users = Objects.requireNonNull(users);
    this.credentials = Objects.requireNonNull(credentials);
    this.passwordHasher = Objects.requireNonNull(passwordHasher);
    this.outbox = Objects.requireNonNull(outbox);
  }

  @Transactional
  public RegisterUserResult register(RegisterUserCommand command) {
    Email email = new Email(command.email());
    Password password = new Password(command.password());
    if (users.existsByEmail(email)) {
      throw new DuplicateEmailException();
    }

    User user = users.save(User.register(email));
    credentials.save(user.id(), passwordHasher.hash(password));
    outbox.save(OutboxEvent.userRegistered(user));

    return new RegisterUserResult(user.id(), user.email().value());
  }
}
