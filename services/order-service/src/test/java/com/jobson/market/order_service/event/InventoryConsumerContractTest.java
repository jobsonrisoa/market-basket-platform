package com.jobson.market.order_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class InventoryConsumerContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @ParameterizedTest(name = "{0}")
  @MethodSource("contracts")
  void shouldAcceptInventoryExampleEvent(Contract contract) throws Exception {
    JsonNode event = read(contract.example());

    assertEquals(contract.eventType(), event.path("eventType").stringValue());
    assertEnvelope(event);
    assertCommonPayload(event.path("payload"));
    contract.assertSpecificPayload(event.path("payload"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("contracts")
  void shouldTolerateAdditivePayloadFields(Contract contract) throws Exception {
    String json =
        Files.readString(contract.example())
            .replace("\"payload\": {", "\"payload\": {\"consumerIgnored\":true,");

    JsonNode event = objectMapper.readTree(json);

    assertCommonPayload(event.path("payload"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("contracts")
  void shouldRejectInventoryEventWithoutReservationId(Contract contract) throws Exception {
    JsonNode payload = read(contract.example()).path("payload");

    assertThrows(
        IllegalArgumentException.class, () -> requireText(payload, "missingReservationId"));
  }

  private static Stream<Contract> contracts() {
    return Stream.of(
        new Contract(
            "inventory.stock_reserved.v1",
            Path.of(
                "../inventory-service/src/test/resources/contracts/inventory/examples/stock-reserved-v1.json"),
            payload -> {}),
        new Contract(
            "inventory.reservation_released.v1",
            Path.of(
                "../inventory-service/src/test/resources/contracts/inventory/examples/reservation-released-v1.json"),
            payload -> requireText(payload, "releasedAt")));
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

  private void assertCommonPayload(JsonNode payload) {
    UUID.fromString(requireText(payload, "stockId"));
    UUID.fromString(requireText(payload, "reservationId"));
    UUID.fromString(requireText(payload, "sellerId"));
    UUID.fromString(requireText(payload, "productId"));
    assertTrue(new BigDecimal(payload.path("quantity").asText()).compareTo(BigDecimal.ZERO) > 0);
    assertTrue(
        new BigDecimal(payload.path("availableQuantity").asText()).compareTo(BigDecimal.ZERO) >= 0);
    requireText(payload, "unit");
    requireText(payload, "requestedBy");
    requireText(payload, "referenceId");
  }

  private static String requireText(JsonNode node, String field) {
    if (!node.has(field) || node.path(field).stringValue().isBlank()) {
      throw new IllegalArgumentException("Missing required field " + field);
    }
    return node.path(field).stringValue();
  }

  private record Contract(String eventType, Path example, PayloadAssertion payloadAssertion) {
    void assertSpecificPayload(JsonNode payload) {
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
