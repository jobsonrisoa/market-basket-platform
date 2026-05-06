package com.jobson.market.auth.application.port.identity;

import java.util.Optional;
import java.util.UUID;

public interface PasswordCredentialRepository {

  void save(UUID userId, String passwordHash);

  Optional<String> findPasswordHashByUserId(UUID userId);
}
