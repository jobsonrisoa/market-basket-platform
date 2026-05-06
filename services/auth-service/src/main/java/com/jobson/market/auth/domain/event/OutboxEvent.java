package com.jobson.market.auth.domain.event;

import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.User;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OutboxEvent(
    UUID eventId,
    String aggregateId,
    String eventType,
    int version,
    Instant occurredAt,
    String correlationId,
    String payload) {

  private static final String USER_REGISTERED = "auth.user.registered.v1";
  private static final String GOOGLE_ACCOUNT_LINKED = "auth.user.google_account_linked.v1";
  private static final String LOGIN_SUCCEEDED = "auth.session.login_succeeded.v1";
  private static final String LOGIN_FAILED = "auth.session.login_failed.v1";
  private static final String REFRESH_TOKEN_ROTATED = "auth.session.refresh_token_rotated.v1";
  private static final String REFRESH_TOKEN_REUSED = "auth.session.refresh_token_reused.v1";
  private static final String SESSION_REVOKED = "auth.session.revoked.v1";

  public OutboxEvent {
    Objects.requireNonNull(eventId, "eventId is required");
    if (aggregateId == null || aggregateId.isBlank()) {
      throw new IllegalArgumentException("aggregateId is required");
    }
    if (eventType == null || eventType.isBlank()) {
      throw new IllegalArgumentException("eventType is required");
    }
    if (version < 1) {
      throw new IllegalArgumentException("version must be positive");
    }
    Objects.requireNonNull(occurredAt, "occurredAt is required");
    if (correlationId == null || correlationId.isBlank()) {
      throw new IllegalArgumentException("correlationId is required");
    }
    if (payload == null || payload.isBlank()) {
      throw new IllegalArgumentException("payload is required");
    }
  }

  public static OutboxEvent userRegistered(User user) {
    String payload =
        "{\"userId\":\"%s\",\"email\":\"%s\"}".formatted(user.id(), user.email().value());
    return new OutboxEvent(
        UUID.randomUUID(),
        user.id().toString(),
        USER_REGISTERED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        payload);
  }

  public static OutboxEvent loginSucceeded(User user) {
    String payload =
        "{\"userId\":\"%s\",\"email\":\"%s\"}".formatted(user.id(), user.email().value());
    return new OutboxEvent(
        UUID.randomUUID(),
        user.id().toString(),
        LOGIN_SUCCEEDED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        payload);
  }

  public static OutboxEvent loginFailed(Email email) {
    String payload = "{\"email\":\"%s\"}".formatted(email.value());
    return new OutboxEvent(
        UUID.randomUUID(),
        email.value(),
        LOGIN_FAILED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        payload);
  }

  public static OutboxEvent googleAccountLinked(User user) {
    String payload =
        "{\"userId\":\"%s\",\"email\":\"%s\"}".formatted(user.id(), user.email().value());
    return new OutboxEvent(
        UUID.randomUUID(),
        user.id().toString(),
        GOOGLE_ACCOUNT_LINKED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        payload);
  }

  public static OutboxEvent refreshTokenRotated(UUID userId, UUID familyId) {
    String payload = "{\"userId\":\"%s\",\"familyId\":\"%s\"}".formatted(userId, familyId);
    return new OutboxEvent(
        UUID.randomUUID(),
        userId.toString(),
        REFRESH_TOKEN_ROTATED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        payload);
  }

  public static OutboxEvent refreshTokenReused(UUID userId, UUID familyId) {
    String payload = "{\"userId\":\"%s\",\"familyId\":\"%s\"}".formatted(userId, familyId);
    return new OutboxEvent(
        UUID.randomUUID(),
        userId.toString(),
        REFRESH_TOKEN_REUSED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        payload);
  }

  public static OutboxEvent sessionRevoked(UUID userId, UUID familyId) {
    String payload = "{\"userId\":\"%s\",\"familyId\":\"%s\"}".formatted(userId, familyId);
    return new OutboxEvent(
        UUID.randomUUID(),
        userId.toString(),
        SESSION_REVOKED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        payload);
  }
}
