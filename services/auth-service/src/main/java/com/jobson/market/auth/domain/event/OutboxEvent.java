package com.jobson.market.auth.domain.event;

import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.Role;
import com.jobson.market.auth.domain.model.User;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record OutboxEvent(
    UUID eventId,
    String aggregateId,
    String eventType,
    int version,
    Instant occurredAt,
    String correlationId,
    Map<String, Object> payload) {

  private static final String USER_REGISTERED = "auth.user.registered.v1";
  private static final String ROLE_ASSIGNED = "auth.user.role_assigned.v1";
  private static final String ROLE_REMOVED = "auth.user.role_removed.v1";
  private static final String ACCOUNT_SUSPENDED = "auth.user.account_suspended.v1";
  private static final String ACCOUNT_REACTIVATED = "auth.user.account_reactivated.v1";
  private static final String GOOGLE_ACCOUNT_LINKED = "auth.user.google_account_linked.v1";
  private static final String LOGIN_SUCCEEDED = "auth.session.login_succeeded.v1";
  private static final String LOGIN_FAILED = "auth.session.login_failed.v1";
  private static final String REFRESH_TOKEN_ROTATED = "auth.session.refresh_token_rotated.v1";
  private static final String REFRESH_TOKEN_REUSED = "auth.session.refresh_token_reused.v1";
  private static final String SESSION_REVOKED = "auth.session.revoked.v1";
  private static final String USER_ID = "userId";

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
    if (payload == null || payload.isEmpty()) {
      throw new IllegalArgumentException("payload is required");
    }
    payload = Map.copyOf(payload);
  }

  public static OutboxEvent userRegistered(User user) {
    return new OutboxEvent(
        UUID.randomUUID(),
        user.id().toString(),
        USER_REGISTERED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        userEmailPayload(user));
  }

  public static OutboxEvent loginSucceeded(User user) {
    return new OutboxEvent(
        UUID.randomUUID(),
        user.id().toString(),
        LOGIN_SUCCEEDED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        userEmailPayload(user));
  }

  public static OutboxEvent loginFailed(Email email) {
    return new OutboxEvent(
        UUID.randomUUID(),
        email.value(),
        LOGIN_FAILED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        payload("email", email.value()));
  }

  public static OutboxEvent roleAssigned(User user, Role role, UUID changedBy) {
    return userRoleChanged(user, role, changedBy, ROLE_ASSIGNED);
  }

  public static OutboxEvent roleRemoved(User user, Role role, UUID changedBy) {
    return userRoleChanged(user, role, changedBy, ROLE_REMOVED);
  }

  public static OutboxEvent accountSuspended(User user, UUID changedBy) {
    return userStateChanged(user, changedBy, ACCOUNT_SUSPENDED);
  }

  public static OutboxEvent accountReactivated(User user, UUID changedBy) {
    return userStateChanged(user, changedBy, ACCOUNT_REACTIVATED);
  }

  private static OutboxEvent userRoleChanged(
      User user, Role role, UUID changedBy, String eventType) {
    return new OutboxEvent(
        UUID.randomUUID(),
        user.id().toString(),
        eventType,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        payload(
            USER_ID,
            user.id().toString(),
            "role",
            role.name(),
            "accountProfile",
            user.accountProfile().name(),
            "changedBy",
            changedBy.toString()));
  }

  private static OutboxEvent userStateChanged(User user, UUID changedBy, String eventType) {
    return new OutboxEvent(
        UUID.randomUUID(),
        user.id().toString(),
        eventType,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        payload(
            USER_ID,
            user.id().toString(),
            "status",
            user.status().name(),
            "changedBy",
            changedBy.toString()));
  }

  public static OutboxEvent googleAccountLinked(User user) {
    return new OutboxEvent(
        UUID.randomUUID(),
        user.id().toString(),
        GOOGLE_ACCOUNT_LINKED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        userEmailPayload(user));
  }

  public static OutboxEvent refreshTokenRotated(UUID userId, UUID familyId) {
    return new OutboxEvent(
        UUID.randomUUID(),
        userId.toString(),
        REFRESH_TOKEN_ROTATED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        refreshTokenPayload(userId, familyId));
  }

  public static OutboxEvent refreshTokenReused(UUID userId, UUID familyId) {
    return new OutboxEvent(
        UUID.randomUUID(),
        userId.toString(),
        REFRESH_TOKEN_REUSED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        refreshTokenPayload(userId, familyId));
  }

  public static OutboxEvent sessionRevoked(UUID userId, UUID familyId) {
    return new OutboxEvent(
        UUID.randomUUID(),
        userId.toString(),
        SESSION_REVOKED,
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        refreshTokenPayload(userId, familyId));
  }

  private static Map<String, Object> userEmailPayload(User user) {
    return payload(USER_ID, user.id().toString(), "email", user.email().value());
  }

  private static Map<String, Object> refreshTokenPayload(UUID userId, UUID familyId) {
    return payload(USER_ID, userId.toString(), "familyId", familyId.toString());
  }

  private static Map<String, Object> payload(Object... entries) {
    if (entries.length % 2 != 0) {
      throw new IllegalArgumentException("payload entries must be key-value pairs");
    }
    Map<String, Object> payload = new LinkedHashMap<>();
    for (int index = 0; index < entries.length; index += 2) {
      payload.put((String) entries[index], Objects.requireNonNull(entries[index + 1]));
    }
    return payload;
  }
}
