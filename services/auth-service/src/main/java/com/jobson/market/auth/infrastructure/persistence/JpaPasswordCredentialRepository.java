package com.jobson.market.auth.infrastructure.persistence;

import com.jobson.market.auth.application.port.identity.PasswordCredentialRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaPasswordCredentialRepository implements PasswordCredentialRepository {

  private final SpringDataPasswordCredentialRepository credentials;
  private final Clock clock;

  JpaPasswordCredentialRepository(SpringDataPasswordCredentialRepository credentials, Clock clock) {
    this.credentials = credentials;
    this.clock = clock;
  }

  @Override
  public void save(UUID userId, String passwordHash) {
    credentials.save(new PasswordCredentialEntity(userId, passwordHash, clock.instant()));
  }

  @Override
  public Optional<String> findPasswordHashByUserId(UUID userId) {
    return credentials.findById(userId).map(PasswordCredentialEntity::passwordHash);
  }
}
