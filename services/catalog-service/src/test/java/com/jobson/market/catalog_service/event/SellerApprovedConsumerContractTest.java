package com.jobson.market.catalog_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SellerApprovedConsumerContractTest {

  private static final Path EXAMPLE =
      Path.of(
          "../seller-service/src/test/resources/contracts/seller/examples/seller-approved-v1.json");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldAcceptSellerApprovedExampleEvent() throws Exception {
    JsonNode event = readExample();

    assertEquals("seller.approved.v1", event.path("eventType").stringValue());
    assertEnvelope(event);
    assertPayload(event.path("payload"));
  }

  @Test
  void shouldTolerateAdditivePayloadFields() throws Exception {
    String json =
        Files.readString(EXAMPLE).replace("\"payload\": {", "\"payload\": {\"tier\":\"LOCAL\",");

    JsonNode event = objectMapper.readTree(json);

    assertPayload(event.path("payload"));
  }

  @Test
  void shouldRejectSellerApprovedEventWithoutSellerId() throws Exception {
    JsonNode payload = readExample().path("payload");

    assertThrows(IllegalArgumentException.class, () -> requireText(payload, "missingSellerId"));
  }

  private JsonNode readExample() throws Exception {
    return objectMapper.readTree(Files.readString(EXAMPLE));
  }

  private void assertEnvelope(JsonNode event) {
    requireText(event, "eventId");
    UUID.fromString(event.path("eventId").stringValue());
    requireText(event, "eventType");
    assertEquals(1, event.path("version").asInt());
    requireText(event, "occurredAt");
    requireText(event, "correlationId");
    assertTrue(event.path("payload").isObject());
  }

  private void assertPayload(JsonNode payload) {
    UUID.fromString(requireText(payload, "sellerId"));
    assertEquals("APPROVED", requireText(payload, "approvalStatus"));
    UUID.fromString(requireText(payload, "ownerUserId"));
    UUID.fromString(requireText(payload, "reviewedByUserId"));
    requireText(payload, "reviewedAt");
    assertFalse(requireText(payload, "name").isBlank());
  }

  private String requireText(JsonNode node, String field) {
    if (!node.has(field) || node.path(field).stringValue().isBlank()) {
      throw new IllegalArgumentException("Missing required field " + field);
    }
    return node.path(field).stringValue();
  }
}
