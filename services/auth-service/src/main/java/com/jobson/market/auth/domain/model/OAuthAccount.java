package com.jobson.market.auth.domain.model;

import java.util.Objects;
import java.util.UUID;

public record OAuthAccount(
    UUID id, UUID userId, String provider, String providerSubject, Email email) {

  public OAuthAccount {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(userId, "userId is required");
    if (provider == null || provider.isBlank()) {
      throw new IllegalArgumentException("provider is required");
    }
    if (providerSubject == null || providerSubject.isBlank()) {
      throw new IllegalArgumentException("providerSubject is required");
    }
    Objects.requireNonNull(email, "email is required");
  }

  public static OAuthAccount linkGoogle(UUID userId, String providerSubject, Email email) {
    return new OAuthAccount(UUID.randomUUID(), userId, "google", providerSubject, email);
  }
}
