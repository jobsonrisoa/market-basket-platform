package com.jobson.market.auth.infrastructure.persistence.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AuthUserRegisteredContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSerializeUserRegisteredEnvelopeThatMatchesConsumerContract() throws Exception {
    User user = User.register(new Email("john@example.com"));
    OutboxEventEntity event = OutboxEventEntity.pending(OutboxEvent.userRegistered(user));
    KafkaOutboxPublisher publisher =
        new KafkaOutboxPublisher(
            null,
            null,
            Clock.fixed(Instant.parse("2026-05-09T12:00:00Z"), ZoneOffset.UTC),
            objectMapper);

    JsonNode envelope = objectMapper.readTree(publisher.envelope(event));
    JsonNode schema =
        objectMapper.readTree(
            getClass().getResourceAsStream("/contracts/auth/user-registered-v1.schema.json"));

    assertMatchesUserRegisteredSchema(envelope, schema);
    assertEquals(user.id().toString(), envelope.at("/payload/userId").asText());
    assertEquals(user.email().value(), envelope.at("/payload/email").asText());
  }

  private void assertMatchesUserRegisteredSchema(JsonNode envelope, JsonNode schema) {
    assertEquals("auth.user.registered.v1", schema.path("title").asText());
    assertEquals("auth.user.registered.v1", envelope.path("eventType").asText());
    assertEquals(1, envelope.path("version").asInt());
    assertObjectHasOnlyFields(
        envelope, "eventId", "eventType", "version", "occurredAt", "correlationId", "payload");
    assertObjectHasOnlyFields(envelope.path("payload"), "userId", "email");
    assertRequiredFieldsExist(envelope, schema.path("required"));
    assertRequiredFieldsExist(envelope.path("payload"), schema.at("/properties/payload/required"));
    assertIsUuid(envelope.path("eventId").asText());
    assertIsUuid(envelope.at("/payload/userId").asText());
    assertFalse(envelope.path("correlationId").asText().isBlank());
    assertFalse(envelope.path("occurredAt").asText().isBlank());
    assertTrue(envelope.at("/payload/email").asText().contains("@"));
  }

  private void assertObjectHasOnlyFields(JsonNode node, String... fields) {
    Set<String> allowed = Set.of(fields);
    node.propertyNames()
        .forEach(field -> assertTrue(allowed.contains(field), "Unexpected field " + field));
    for (String field : fields) {
      assertTrue(node.has(field), "Missing field " + field);
    }
  }

  private void assertRequiredFieldsExist(JsonNode node, JsonNode requiredFields) {
    requiredFields.forEach(
        field -> assertTrue(node.has(field.asText()), "Missing field " + field.asText()));
  }

  private void assertIsUuid(String value) {
    UUID.fromString(value);
  }
}
