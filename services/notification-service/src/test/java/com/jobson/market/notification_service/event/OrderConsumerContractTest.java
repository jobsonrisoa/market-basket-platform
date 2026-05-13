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

class OrderConsumerContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @ParameterizedTest(name = "{0}")
  @MethodSource("contracts")
  void shouldAcceptOrderExampleEvent(Contract contract) throws Exception {
    JsonNode event = read(contract.example());

    assertEquals(contract.eventType(), event.path("eventType").stringValue());
    assertEnvelope(event);
    assertPayload(event.path("payload"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("contracts")
  void shouldTolerateAdditivePayloadFields(Contract contract) throws Exception {
    String json =
        Files.readString(contract.example())
            .replace("\"payload\": {", "\"payload\": {\"ignored\":true,");

    assertPayload(objectMapper.readTree(json).path("payload"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("contracts")
  void shouldRejectOrderEventWithoutOrderId(Contract contract) throws Exception {
    JsonNode payload = read(contract.example()).path("payload");

    assertThrows(IllegalArgumentException.class, () -> requireText(payload, "missingOrderId"));
  }

  private static Stream<Contract> contracts() {
    return Stream.of(
        new Contract(
            "order.confirmed.v1",
            Path.of("../order-service/src/test/resources/contracts/order/examples/order-confirmed-v1.json")),
        new Contract(
            "order.fulfillment_status_changed.v1",
            Path.of(
                "../order-service/src/test/resources/contracts/order/examples/order-fulfillment-status-changed-v1.json")));
  }

  private JsonNode read(Path example) throws Exception {
    return objectMapper.readTree(Files.readString(example));
  }

  private void assertEnvelope(JsonNode event) {
    UUID.fromString(requireText(event, "eventId"));
    assertEquals(1, event.path("version").asInt());
    requireText(event, "occurredAt");
    requireText(event, "correlationId");
    assertTrue(event.path("payload").isObject());
  }

  private void assertPayload(JsonNode payload) {
    UUID.fromString(requireText(payload, "orderId"));
    UUID.fromString(requireText(payload, "customerId"));
    UUID.fromString(requireText(payload, "sellerId"));
    UUID.fromString(requireText(payload, "productId"));
    UUID.fromString(requireText(payload, "stockId"));
    requireText(payload, "unit");
    requireText(payload, "status");
    requireText(payload, "fulfillmentStatus");
  }

  private static String requireText(JsonNode node, String field) {
    if (!node.has(field) || node.path(field).stringValue().isBlank()) {
      throw new IllegalArgumentException("Missing required field " + field);
    }
    return node.path(field).stringValue();
  }

  private record Contract(String eventType, Path example) {
    @Override
    public String toString() {
      return eventType;
    }
  }
}
