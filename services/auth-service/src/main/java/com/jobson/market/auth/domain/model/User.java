package com.jobson.market.auth.domain.model;

import java.util.Objects;
import java.util.UUID;

public record User(UUID id, Email email, boolean emailVerified, UserStatus status) {

  public User {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(email, "email is required");
    Objects.requireNonNull(status, "status is required");
  }

  public static User register(Email email) {
    return new User(UUID.randomUUID(), email, false, UserStatus.PENDING_EMAIL_VERIFICATION);
  }

  public User verifyEmail() {
    return new User(id, email, true, UserStatus.ACTIVE);
  }
}
