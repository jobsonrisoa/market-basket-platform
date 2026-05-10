package com.jobson.market.auth.infrastructure.persistence.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.Role;
import com.jobson.market.auth.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AuthEventContractTest {

  private static final User USER = User.register(new Email("john@example.com"));
  private static final UUID FAMILY_ID = UUID.fromString("8b5ae8b4-932d-4e88-8136-21e6b0934483");
  private static final UUID CHANGED_BY = UUID.fromString("d2d9cc06-c4f9-476d-9496-97422ed73345");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @ParameterizedTest(name = "{0}")
  @MethodSource("contracts")
  void shouldSerializeAuthEnvelopeThatMatchesConsumerContract(ContractCase contract)
      throws Exception {
    OutboxEvent event = contract.event();
    OutboxEventEntity entity =
        OutboxEventEntity.pending(event, objectMapper.writeValueAsString(event.payload()));
    KafkaOutboxPublisher publisher =
        new KafkaOutboxPublisher(
            null,
            null,
            Clock.fixed(Instant.parse("2026-05-09T12:00:00Z"), ZoneOffset.UTC),
            objectMapper);

    JsonNode envelope = objectMapper.readTree(publisher.envelope(entity));
    JsonNode schema =
        objectMapper.readTree(getClass().getResourceAsStream(contract.schemaResource()));

    assertMatchesSchema(envelope, schema, contract.payloadFields());
    contract
        .expectedPayloadValues()
        .forEach(
            (field, expectedValue) ->
                assertEquals(expectedValue, envelope.at("/payload/" + field).stringValue()));
  }

  private static Stream<ContractCase> contracts() {
    User seller = USER.assignRole(Role.SELLER_OWNER);
    User suspended = USER.suspend();
    return Stream.of(
        contract(
            "auth.user.registered.v1",
            "/contracts/auth/user-registered-v1.schema.json",
            OutboxEvent.userRegistered(USER),
            Map.of("userId", USER.id().toString(), "email", USER.email().value()),
            "userId",
            "email"),
        contract(
            "auth.session.login_succeeded.v1",
            "/contracts/auth/session-login-succeeded-v1.schema.json",
            OutboxEvent.loginSucceeded(USER),
            Map.of("userId", USER.id().toString(), "email", USER.email().value()),
            "userId",
            "email"),
        contract(
            "auth.session.login_failed.v1",
            "/contracts/auth/session-login-failed-v1.schema.json",
            OutboxEvent.loginFailed(USER.email()),
            Map.of("email", USER.email().value()),
            "email"),
        contract(
            "auth.session.refresh_token_rotated.v1",
            "/contracts/auth/session-refresh-token-rotated-v1.schema.json",
            OutboxEvent.refreshTokenRotated(USER.id(), FAMILY_ID),
            Map.of("userId", USER.id().toString(), "familyId", FAMILY_ID.toString()),
            "userId",
            "familyId"),
        contract(
            "auth.session.refresh_token_reused.v1",
            "/contracts/auth/session-refresh-token-reused-v1.schema.json",
            OutboxEvent.refreshTokenReused(USER.id(), FAMILY_ID),
            Map.of("userId", USER.id().toString(), "familyId", FAMILY_ID.toString()),
            "userId",
            "familyId"),
        contract(
            "auth.session.revoked.v1",
            "/contracts/auth/session-revoked-v1.schema.json",
            OutboxEvent.sessionRevoked(USER.id(), FAMILY_ID),
            Map.of("userId", USER.id().toString(), "familyId", FAMILY_ID.toString()),
            "userId",
            "familyId"),
        contract(
            "auth.user.role_assigned.v1",
            "/contracts/auth/user-role-assigned-v1.schema.json",
            OutboxEvent.roleAssigned(seller, Role.SELLER_OWNER, CHANGED_BY),
            Map.of(
                "userId",
                seller.id().toString(),
                "role",
                Role.SELLER_OWNER.name(),
                "accountProfile",
                seller.accountProfile().name(),
                "changedBy",
                CHANGED_BY.toString()),
            "userId",
            "role",
            "accountProfile",
            "changedBy"),
        contract(
            "auth.user.role_removed.v1",
            "/contracts/auth/user-role-removed-v1.schema.json",
            OutboxEvent.roleRemoved(seller, Role.SELLER_OWNER, CHANGED_BY),
            Map.of(
                "userId",
                seller.id().toString(),
                "role",
                Role.SELLER_OWNER.name(),
                "accountProfile",
                seller.accountProfile().name(),
                "changedBy",
                CHANGED_BY.toString()),
            "userId",
            "role",
            "accountProfile",
            "changedBy"),
        contract(
            "auth.user.account_suspended.v1",
            "/contracts/auth/user-account-suspended-v1.schema.json",
            OutboxEvent.accountSuspended(suspended, CHANGED_BY),
            Map.of(
                "userId",
                suspended.id().toString(),
                "status",
                suspended.status().name(),
                "changedBy",
                CHANGED_BY.toString()),
            "userId",
            "status",
            "changedBy"),
        contract(
            "auth.user.account_reactivated.v1",
            "/contracts/auth/user-account-reactivated-v1.schema.json",
            OutboxEvent.accountReactivated(suspended.reactivate(), CHANGED_BY),
            Map.of(
                "userId",
                suspended.id().toString(),
                "status",
                "ACTIVE",
                "changedBy",
                CHANGED_BY.toString()),
            "userId",
            "status",
            "changedBy"),
        contract(
            "auth.user.google_account_linked.v1",
            "/contracts/auth/user-google-account-linked-v1.schema.json",
            OutboxEvent.googleAccountLinked(USER),
            Map.of("userId", USER.id().toString(), "email", USER.email().value()),
            "userId",
            "email"));
  }

  private static ContractCase contract(
      String eventType,
      String schemaResource,
      OutboxEvent event,
      Map<String, String> expectedPayloadValues,
      String... payloadFields) {
    assertEquals(eventType, event.eventType());
    return new ContractCase(eventType, schemaResource, event, expectedPayloadValues, payloadFields);
  }

  private void assertMatchesSchema(JsonNode envelope, JsonNode schema, String... payloadFields) {
    assertEquals(schema.path("title").stringValue(), envelope.path("eventType").stringValue());
    assertEquals(
        schema.at("/properties/eventType/const").stringValue(),
        envelope.path("eventType").stringValue());
    assertEquals(schema.at("/properties/version/const").asInt(), envelope.path("version").asInt());
    assertObjectHasOnlyFields(
        envelope, "eventId", "eventType", "version", "occurredAt", "correlationId", "payload");
    assertObjectHasOnlyFields(envelope.path("payload"), payloadFields);
    assertRequiredFieldsExist(envelope, schema.path("required"));
    assertRequiredFieldsExist(envelope.path("payload"), schema.at("/properties/payload/required"));
    assertIsUuid(envelope.path("eventId").stringValue());
    assertFalse(envelope.path("correlationId").stringValue().isBlank());
    assertFalse(envelope.path("occurredAt").stringValue().isBlank());
    assertTypedPayloadFields(envelope.path("payload"));
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
        field -> assertTrue(node.has(field.stringValue()), "Missing field " + field.stringValue()));
  }

  private void assertTypedPayloadFields(JsonNode payload) {
    if (payload.has("userId")) {
      assertIsUuid(payload.path("userId").stringValue());
    }
    if (payload.has("familyId")) {
      assertIsUuid(payload.path("familyId").stringValue());
    }
    if (payload.has("changedBy")) {
      assertIsUuid(payload.path("changedBy").stringValue());
    }
    if (payload.has("email")) {
      assertTrue(payload.path("email").stringValue().contains("@"));
    }
  }

  private void assertIsUuid(String value) {
    UUID.fromString(value);
  }

  private record ContractCase(
      String eventType,
      String schemaResource,
      OutboxEvent event,
      Map<String, String> expectedPayloadValues,
      String... payloadFields) {

    @Override
    public String toString() {
      return eventType;
    }
  }
}
