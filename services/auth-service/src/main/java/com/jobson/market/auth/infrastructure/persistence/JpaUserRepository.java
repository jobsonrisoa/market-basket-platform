package com.jobson.market.auth.infrastructure.persistence;

import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaUserRepository implements UserRepository {

  private final SpringDataUserRepository users;
  private final Clock clock;

  JpaUserRepository(SpringDataUserRepository users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  @Override
  public boolean existsByEmail(Email email) {
    return users.existsByEmail(email.value());
  }

  @Override
  public Optional<User> findByEmail(Email email) {
    return users.findByEmail(email.value()).map(this::toDomain);
  }

  @Override
  public Optional<User> findById(UUID id) {
    return users.findById(id).map(this::toDomain);
  }

  @Override
  public User save(User user) {
    UserEntity existing = users.findById(user.id()).orElse(null);
    Instant now = clock.instant();
    Instant createdAt = existing == null ? now : now;
    return toDomain(
        users.save(
            new UserEntity(
                user.id(),
                user.email().value(),
                user.emailVerified(),
                user.status(),
                createdAt,
                now)));
  }

  private User toDomain(UserEntity entity) {
    return new User(
        entity.id(), new Email(entity.email()), entity.emailVerified(), entity.status());
  }
}
