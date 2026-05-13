package com.jobson.market.notification_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class NotificationConsumerContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @ParameterizedTest(name = "{0}")
  @MethodSource("contracts")
  void shouldAcceptNotificationExampleEvent(Contract contract) throws Exception {
    JsonNode event = read(contract.example());

    assertEquals(contract.eventType(), event.path("eventType").stringValue());
    assertEnvelope(event);
    contract.assertPayload(event.path("payload"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("contracts")
  void shouldTolerateAdditivePayloadFields(Contract contract) throws Exception {
    String json =
        Files.readString(contract.example())
            .replace("\"payload\": {", "\"payload\": {\"notificationIgnored\":true,");

    contract.assertPayload(objectMapper.readTree(json).path("payload"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("contracts")
  void shouldRejectNotificationEventWithoutRequiredRecipient(Contract contract) throws Exception {
    JsonNode payload = read(contract.example()).path("payload");

    assertThrows(
        IllegalArgumentException.class, () -> requireMissingField(payload, contract));
  }

  private static Stream<Contract> contracts() {
    return Stream.of(
        new Contract(
            "auth.user.registered.v1",
            Path.of(
                "../auth-service/src/test/resources/contracts/auth/examples/user-registered-v1.json"),
            "missingUserId",
            payload -> {
              UUID.fromString(requireText(payload, "userId"));
              assertTrue(requireText(payload, "email").contains("@"));
            }),
        new Contract(
            "seller.approved.v1",
            Path.of(
                "../seller-service/src/test/resources/contracts/seller/examples/seller-approved-v1.json"),
            "missingSellerId",
            payload -> {
              UUID.fromString(requireText(payload, "sellerId"));
              UUID.fromString(requireText(payload, "ownerUserId"));
              assertEquals("APPROVED", requireText(payload, "approvalStatus"));
              requireText(payload, "name");
            }));
  }

  private JsonNode read(Path example) throws Exception {
    return objectMapper.readTree(Files.readString(example));
  }

  private void assertEnvelope(JsonNode event) {
    UUID.fromString(requireText(event, "eventId"));
    requireText(event, "eventType");
    assertEquals(1, event.path("version").asInt());
    requireText(event, "occurredAt");
    requireText(event, "correlationId");
    assertTrue(event.path("payload").isObject());
  }

  private static String requireText(JsonNode node, String field) {
    if (!node.has(field) || node.path(field).stringValue().isBlank()) {
      throw new IllegalArgumentException("Missing required field " + field);
    }
    return node.path(field).stringValue();
  }

  private static void requireMissingField(JsonNode payload, Contract contract) {
    requireText(payload, contract.missingField());
  }

  private record Contract(
      String eventType, Path example, String missingField, PayloadAssertion payloadAssertion) {
    void assertPayload(JsonNode payload) {
      payloadAssertion.assertPayload(payload);
    }

    @Override
    public String toString() {
      return eventType;
    }
  }

  private interface PayloadAssertion {
    void assertPayload(JsonNode payload);
  }
}
