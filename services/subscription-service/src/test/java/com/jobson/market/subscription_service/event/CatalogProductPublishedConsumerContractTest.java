package com.jobson.market.subscription_service.event;

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

class CatalogProductPublishedConsumerContractTest {

  private static final Path EXAMPLE =
      Path.of(
          "../catalog-service/src/test/resources/contracts/catalog/examples/product-published-v1.json");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldAcceptCatalogProductPublishedExampleEvent() throws Exception {
    JsonNode event = readExample();

    assertEquals("catalog.product.published.v1", event.path("eventType").stringValue());
    assertEnvelope(event);
    assertPayload(event.path("payload"));
  }

  @Test
  void shouldTolerateAdditivePayloadFields() throws Exception {
    String json =
        Files.readString(EXAMPLE).replace("\"payload\": {", "\"payload\": {\"season\":\"SPRING\",");

    assertPayload(objectMapper.readTree(json).path("payload"));
  }

  @Test
  void shouldRejectProductPublishedEventWithoutProductId() throws Exception {
    JsonNode payload = readExample().path("payload");

    assertThrows(IllegalArgumentException.class, () -> requireText(payload, "missingProductId"));
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
    UUID.fromString(requireText(payload, "productId"));
    UUID.fromString(requireText(payload, "sellerId"));
    UUID.fromString(requireText(payload, "categoryId"));
    assertEquals("PUBLISHED", requireText(payload, "status"));
    assertTrue(
        new BigDecimal(payload.path("priceAmount").asText()).compareTo(BigDecimal.ZERO) >= 0);
    requireText(payload, "currency");
    requireText(payload, "unit");
    requireText(payload, "packageSize");
    requireText(payload, "name");
  }

  private String requireText(JsonNode node, String field) {
    if (!node.has(field) || node.path(field).stringValue().isBlank()) {
      throw new IllegalArgumentException("Missing required field " + field);
    }
    return node.path(field).stringValue();
  }
}
