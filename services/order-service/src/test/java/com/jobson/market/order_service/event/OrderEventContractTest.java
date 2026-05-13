package com.jobson.market.order_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.order_service.domain.DraftOrderDetails;
import com.jobson.market.order_service.domain.OrderEntity;
import com.jobson.market.order_service.domain.OrderFulfillmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class OrderEventContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSerializeOrderConfirmedEnvelopeThatMatchesConsumerContract() throws Exception {
    OrderEntity order = confirmedOrder();
    OrderEvent event = OrderEvent.confirmed(order);
    JsonNode envelope = objectMapper.valueToTree(event);
    JsonNode schema =
        objectMapper.readTree(
            getClass().getResourceAsStream("/contracts/order/order-confirmed-v1.schema.json"));

    assertEnvelope(envelope, schema, "order.confirmed.v1");
    assertObjectHasOnlyFields(
        envelope.path("payload"),
        "orderId",
        "customerId",
        "sellerId",
        "productId",
        "stockId",
        "quantity",
        "unit",
        "status",
        "fulfillmentStatus",
        "confirmedAt",
        "fulfilledAt");
    assertEquals("CONFIRMED", envelope.at("/payload/status").stringValue());
  }

  @Test
  void shouldSerializeFulfillmentStatusChangedEnvelopeThatMatchesConsumerContract()
      throws Exception {
    OrderEntity order = confirmedOrder();
    order.changeFulfillmentStatus(
        OrderFulfillmentStatus.FULFILLMENT_READY, Instant.parse("2026-05-13T13:00:00Z"));
    OrderEvent event = OrderEvent.fulfillmentStatusChanged(order);
    JsonNode envelope = objectMapper.valueToTree(event);
    JsonNode schema =
        objectMapper.readTree(
            getClass()
                .getResourceAsStream(
                    "/contracts/order/order-fulfillment-status-changed-v1.schema.json"));

    assertEnvelope(envelope, schema, "order.fulfillment_status_changed.v1");
    assertEquals("FULFILLMENT_READY", envelope.at("/payload/fulfillmentStatus").stringValue());
  }

  private OrderEntity confirmedOrder() {
    OrderEntity order =
        OrderEntity.draft(
            new DraftOrderDetails(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("2.5"),
                "kg",
                "subscription-service",
                "subscription-123"),
            Instant.parse("2026-05-13T12:00:00Z"));
    order.confirm(Instant.parse("2026-05-13T12:05:00Z"));
    return order;
  }

  private void assertEnvelope(JsonNode envelope, JsonNode schema, String eventType) {
    assertEquals(eventType, envelope.path("eventType").stringValue());
    assertEquals(schema.at("/properties/eventType/const").stringValue(), eventType);
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
