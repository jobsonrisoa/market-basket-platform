package unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.Role;
import com.jobson.market.auth.domain.model.User;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

  @Test
  void shouldCreateUserAndSessionEventsWithExpectedPayloads() {
    User user = User.register(new Email("john@example.com"));
    UUID familyId = UUID.randomUUID();

    assertEvent(OutboxEvent.userRegistered(user), "auth.user.registered.v1", user.id().toString());
    assertEvent(
        OutboxEvent.loginSucceeded(user), "auth.session.login_succeeded.v1", user.id().toString());
    assertEvent(
        OutboxEvent.loginFailed(user.email()),
        "auth.session.login_failed.v1",
        user.email().value());
    assertEvent(
        OutboxEvent.refreshTokenRotated(user.id(), familyId),
        "auth.session.refresh_token_rotated.v1",
        user.id().toString());
    assertEvent(
        OutboxEvent.refreshTokenReused(user.id(), familyId),
        "auth.session.refresh_token_reused.v1",
        user.id().toString());
    assertEvent(
        OutboxEvent.sessionRevoked(user.id(), familyId),
        "auth.session.revoked.v1",
        user.id().toString());
  }

  @Test
  void shouldCreateAccountManagementEvents() {
    User user = User.register(new Email("john@example.com")).assignRole(Role.ADMIN).suspend();
    UUID changedBy = UUID.randomUUID();

    assertEvent(
        OutboxEvent.roleAssigned(user, Role.ADMIN, changedBy),
        "auth.user.role_assigned.v1",
        user.id().toString());
    assertEvent(
        OutboxEvent.roleRemoved(user, Role.ADMIN, changedBy),
        "auth.user.role_removed.v1",
        user.id().toString());
    assertEvent(
        OutboxEvent.accountSuspended(user, changedBy),
        "auth.user.account_suspended.v1",
        user.id().toString());
    assertEvent(
        OutboxEvent.accountReactivated(user.reactivate(), changedBy),
        "auth.user.account_reactivated.v1",
        user.id().toString());
    assertEvent(
        OutboxEvent.googleAccountLinked(user),
        "auth.user.google_account_linked.v1",
        user.id().toString());
  }

  @Test
  void shouldRejectInvalidEventData() {
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-05-08T12:00:00Z");
    Map<String, Object> payload = Map.of("id", "1");

    assertThrows(NullPointerException.class, () -> eventWithNullEventId(occurredAt, payload));
    assertThrows(
        IllegalArgumentException.class, () -> eventWithBlankAggregateId(eventId, occurredAt, payload));
    assertThrows(
        IllegalArgumentException.class, () -> eventWithBlankEventType(eventId, occurredAt, payload));
    assertThrows(
        IllegalArgumentException.class, () -> eventWithInvalidVersion(eventId, occurredAt, payload));
    assertThrows(NullPointerException.class, () -> eventWithNullOccurredAt(eventId, payload));
    assertThrows(
        IllegalArgumentException.class, () -> eventWithBlankCorrelationId(eventId, occurredAt, payload));
    assertThrows(
        IllegalArgumentException.class,
        () -> eventWithEmptyPayload(eventId, occurredAt));
  }

  private OutboxEvent eventWithNullEventId(Instant occurredAt, Map<String, Object> payload) {
    return new OutboxEvent(null, "id", "type", 1, occurredAt, "correlation", payload);
  }

  private OutboxEvent eventWithBlankAggregateId(
      UUID eventId, Instant occurredAt, Map<String, Object> payload) {
    return new OutboxEvent(eventId, " ", "type", 1, occurredAt, "correlation", payload);
  }

  private OutboxEvent eventWithBlankEventType(
      UUID eventId, Instant occurredAt, Map<String, Object> payload) {
    return new OutboxEvent(eventId, "id", " ", 1, occurredAt, "correlation", payload);
  }

  private OutboxEvent eventWithInvalidVersion(
      UUID eventId, Instant occurredAt, Map<String, Object> payload) {
    return new OutboxEvent(eventId, "id", "type", 0, occurredAt, "correlation", payload);
  }

  private OutboxEvent eventWithNullOccurredAt(UUID eventId, Map<String, Object> payload) {
    return new OutboxEvent(eventId, "id", "type", 1, null, "correlation", payload);
  }

  private OutboxEvent eventWithBlankCorrelationId(
      UUID eventId, Instant occurredAt, Map<String, Object> payload) {
    return new OutboxEvent(eventId, "id", "type", 1, occurredAt, " ", payload);
  }

  private OutboxEvent eventWithEmptyPayload(UUID eventId, Instant occurredAt) {
    return new OutboxEvent(eventId, "id", "type", 1, occurredAt, "correlation", Map.of());
  }

  private void assertEvent(OutboxEvent event, String eventType, String aggregateId) {
    assertEquals(eventType, event.eventType());
    assertEquals(aggregateId, event.aggregateId());
    assertEquals(1, event.version());
    assertTrue(event.payload().containsValue(aggregateId));
  }
}
