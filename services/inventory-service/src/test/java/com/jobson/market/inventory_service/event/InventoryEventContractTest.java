package com.jobson.market.inventory_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.inventory_service.domain.InventoryReservationEntity;
import com.jobson.market.inventory_service.domain.InventoryStockEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class InventoryEventContractTest {

  private static final UUID SELLER_ID = UUID.fromString("2f23681e-8e7f-4819-a7dd-ef4f88522921");
  private static final UUID PRODUCT_ID = UUID.fromString("7f3a7d58-e531-4ca8-918e-57c4663888c8");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSerializeStockReservedEnvelopeThatMatchesConsumerContract() throws Exception {
    InventoryStockEntity stock =
        InventoryStockEntity.create(
            SELLER_ID,
            PRODUCT_ID,
            new BigDecimal("10.00"),
            "kg",
            Instant.parse("2026-05-10T12:00:00Z"));
    InventoryReservationEntity reservation =
        InventoryReservationEntity.active(
            stock,
            new BigDecimal("3.50"),
            "order-service",
            "order-123",
            Instant.parse("2026-05-10T13:00:00Z"));

    InventoryEvent event = InventoryEvent.stockReserved(stock, reservation);
    JsonNode envelope = objectMapper.valueToTree(event);
    JsonNode schema =
        objectMapper.readTree(
            getClass().getResourceAsStream("/contracts/inventory/stock-reserved-v1.schema.json"));

    assertEnvelopeMatchesSchema(envelope, schema, "inventory.stock_reserved.v1");
    assertObjectHasOnlyFields(
        envelope.path("payload"),
        "stockId",
        "reservationId",
        "sellerId",
        "productId",
        "quantity",
        "unit",
        "availableQuantity",
        "requestedBy",
        "referenceId");
    assertEquals(stock.id().toString(), envelope.at("/payload/stockId").stringValue());
    assertEquals(reservation.id().toString(), envelope.at("/payload/reservationId").stringValue());
    assertEquals(
        new BigDecimal("6.50"), new BigDecimal(envelope.at("/payload/availableQuantity").asText()));
  }

  @Test
  void shouldSerializeReservationReleasedEnvelopeThatMatchesConsumerContract() throws Exception {
    InventoryStockEntity stock =
        InventoryStockEntity.create(
            SELLER_ID,
            PRODUCT_ID,
            new BigDecimal("10.00"),
            "kg",
            Instant.parse("2026-05-10T12:00:00Z"));
    InventoryReservationEntity reservation =
        InventoryReservationEntity.active(
            stock,
            new BigDecimal("3.50"),
            "order-service",
            "order-123",
            Instant.parse("2026-05-10T13:00:00Z"));
    reservation.release(Instant.parse("2026-05-10T14:00:00Z"));
    stock.release(reservation.quantity(), Instant.parse("2026-05-10T14:00:00Z"));

    InventoryEvent event = InventoryEvent.reservationReleased(stock, reservation);
    JsonNode envelope = objectMapper.valueToTree(event);
    JsonNode schema =
        objectMapper.readTree(
            getClass()
                .getResourceAsStream("/contracts/inventory/reservation-released-v1.schema.json"));

    assertEnvelopeMatchesSchema(envelope, schema, "inventory.reservation_released.v1");
    assertObjectHasOnlyFields(
        envelope.path("payload"),
        "stockId",
        "reservationId",
        "sellerId",
        "productId",
        "quantity",
        "unit",
        "availableQuantity",
        "requestedBy",
        "referenceId",
        "releasedAt");
    assertEquals(
        new BigDecimal("10.00"),
        new BigDecimal(envelope.at("/payload/availableQuantity").asText()));
  }

  private void assertEnvelopeMatchesSchema(JsonNode envelope, JsonNode schema, String eventType) {
    assertEquals(eventType, envelope.path("eventType").stringValue());
    assertEquals(schema.path("title").stringValue(), envelope.path("eventType").stringValue());
    assertEquals(
        schema.at("/properties/eventType/const").stringValue(),
        envelope.path("eventType").stringValue());
    assertEquals(schema.at("/properties/version/const").asInt(), envelope.path("version").asInt());
    assertObjectHasOnlyFields(
        envelope, "eventId", "eventType", "version", "occurredAt", "correlationId", "payload");
    assertRequiredFieldsExist(envelope, schema.path("required"));
    assertRequiredFieldsExist(envelope.path("payload"), schema.at("/properties/payload/required"));
    UUID.fromString(envelope.path("eventId").stringValue());
    assertFalse(envelope.path("correlationId").stringValue().isBlank());
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
}
