package com.jobson.market.catalog_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.catalog_service.domain.ProductEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class CatalogProductPublishedContractTest {

  private static final UUID SELLER_ID = UUID.fromString("2f23681e-8e7f-4819-a7dd-ef4f88522921");
  private static final UUID CATEGORY_ID = UUID.fromString("7f3a7d58-e531-4ca8-918e-57c4663888c8");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSerializeProductPublishedEnvelopeThatMatchesConsumerContract() throws Exception {
    ProductEntity product =
        ProductEntity.create(
            SELLER_ID,
            CATEGORY_ID,
            "Organic Carrots",
            "Fresh carrots",
            "kg",
            "1 kg bag",
            new BigDecimal("7.50"),
            "USD",
            Instant.parse("2026-05-10T12:00:00Z"));
    product.publish(Instant.parse("2026-05-10T13:00:00Z"));

    CatalogEvent event = CatalogEvent.productPublished(product);
    JsonNode envelope = objectMapper.valueToTree(event);
    JsonNode schema =
        objectMapper.readTree(
            getClass().getResourceAsStream("/contracts/catalog/product-published-v1.schema.json"));

    assertEquals("catalog.product.published.v1", event.eventType());
    assertEquals(schema.path("title").stringValue(), envelope.path("eventType").stringValue());
    assertEquals(
        schema.at("/properties/eventType/const").stringValue(),
        envelope.path("eventType").stringValue());
    assertEquals(schema.at("/properties/version/const").asInt(), envelope.path("version").asInt());
    assertObjectHasOnlyFields(
        envelope, "eventId", "eventType", "version", "occurredAt", "correlationId", "payload");
    assertObjectHasOnlyFields(
        envelope.path("payload"),
        "productId",
        "sellerId",
        "categoryId",
        "name",
        "unit",
        "packageSize",
        "priceAmount",
        "currency",
        "status");
    assertRequiredFieldsExist(envelope, schema.path("required"));
    assertRequiredFieldsExist(envelope.path("payload"), schema.at("/properties/payload/required"));
    UUID.fromString(envelope.path("eventId").stringValue());
    assertFalse(envelope.path("correlationId").stringValue().isBlank());
    assertEquals(product.id().toString(), envelope.at("/payload/productId").stringValue());
    assertEquals(SELLER_ID.toString(), envelope.at("/payload/sellerId").stringValue());
    assertEquals(CATEGORY_ID.toString(), envelope.at("/payload/categoryId").stringValue());
    assertEquals("PUBLISHED", envelope.at("/payload/status").stringValue());
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
