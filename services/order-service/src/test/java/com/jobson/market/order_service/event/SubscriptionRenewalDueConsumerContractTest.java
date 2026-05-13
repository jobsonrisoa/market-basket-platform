package com.jobson.market.order_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SubscriptionRenewalDueConsumerContractTest {

  private static final Path EXAMPLE =
      Path.of(
          "../subscription-service/src/test/resources/contracts/subscription/examples/subscription-renewal-due-v1.json");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldAcceptSubscriptionRenewalDueExampleEvent() throws Exception {
    JsonNode event = readExample();

    assertEquals("subscription.renewal_due.v1", event.path("eventType").stringValue());
    assertEnvelope(event);
    assertPayload(event.path("payload"));
  }

  @Test
  void shouldTolerateAdditivePayloadFields() throws Exception {
    String json =
        Files.readString(EXAMPLE).replace("\"payload\": {", "\"payload\": {\"ignored\":true,");

    assertPayload(objectMapper.readTree(json).path("payload"));
  }

  @Test
  void shouldRejectRenewalDueEventWithoutDraftOrderId() throws Exception {
    JsonNode payload = readExample().path("payload");

    assertThrows(IllegalArgumentException.class, () -> requireText(payload, "missingDraftOrderId"));
  }

  private JsonNode readExample() throws Exception {
    return objectMapper.readTree(Files.readString(EXAMPLE));
  }

  private void assertEnvelope(JsonNode event) {
    UUID.fromString(requireText(event, "eventId"));
    assertEquals(1, event.path("version").asInt());
    requireText(event, "occurredAt");
    requireText(event, "correlationId");
    assertTrue(event.path("payload").isObject());
  }

  private void assertPayload(JsonNode payload) {
    UUID.fromString(requireText(payload, "subscriptionId"));
    UUID.fromString(requireText(payload, "customerId"));
    UUID.fromString(requireText(payload, "sellerId"));
    UUID.fromString(requireText(payload, "productId"));
    UUID.fromString(requireText(payload, "stockId"));
    UUID.fromString(requireText(payload, "draftOrderId"));
    assertTrue(new BigDecimal(payload.path("quantity").toString()).compareTo(BigDecimal.ZERO) > 0);
    requireText(payload, "unit");
    requireText(payload, "basketSize");
    requireText(payload, "cadence");
    requireText(payload, "nextRenewalDate");
  }

  private String requireText(JsonNode node, String field) {
    if (!node.has(field) || node.path(field).stringValue().isBlank()) {
      throw new IllegalArgumentException("Missing required field " + field);
    }
    return node.path(field).stringValue();
  }
}
