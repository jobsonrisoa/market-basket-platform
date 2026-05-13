package com.jobson.market.subscription_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.subscription_service.domain.SubscriptionEntity;
import com.jobson.market.subscription_service.domain.SubscriptionPlanDetails;
import com.jobson.market.subscription_service.domain.SubscriptionPlanEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SubscriptionRenewalDueContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSerializeRenewalDueEnvelopeThatMatchesConsumerContract() throws Exception {
    SubscriptionPlanEntity plan =
        SubscriptionPlanEntity.create(
            new SubscriptionPlanDetails(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SMALL",
                "WEEKLY",
                new BigDecimal("2.5"),
                "kg"),
            Instant.parse("2026-05-13T12:00:00Z"));
    SubscriptionEntity subscription =
        SubscriptionEntity.active(
            UUID.randomUUID(),
            plan,
            LocalDate.parse("2026-05-20"),
            UUID.randomUUID(),
            Instant.parse("2026-05-13T12:00:00Z"));
    SubscriptionEvent event = SubscriptionEvent.renewalDue(subscription);
    JsonNode envelope = objectMapper.valueToTree(event);
    JsonNode schema =
        objectMapper.readTree(
            getClass()
                .getResourceAsStream(
                    "/contracts/subscription/subscription-renewal-due-v1.schema.json"));

    assertEquals("subscription.renewal_due.v1", envelope.path("eventType").stringValue());
    assertEquals(
        schema.at("/properties/eventType/const").stringValue(),
        envelope.path("eventType").stringValue());
    assertEquals(schema.at("/properties/version/const").asInt(), envelope.path("version").asInt());
    assertObjectHasOnlyFields(
        envelope, "eventId", "eventType", "version", "occurredAt", "correlationId", "payload");
    assertObjectHasOnlyFields(
        envelope.path("payload"),
        "subscriptionId",
        "customerId",
        "sellerId",
        "productId",
        "stockId",
        "draftOrderId",
        "basketSize",
        "cadence",
        "quantity",
        "unit",
        "nextRenewalDate",
        "status");
    assertRequiredFieldsExist(envelope, schema.path("required"));
    assertRequiredFieldsExist(envelope.path("payload"), schema.at("/properties/payload/required"));
    UUID.fromString(envelope.path("eventId").stringValue());
    assertFalse(envelope.path("correlationId").stringValue().isBlank());
    assertEquals("ACTIVE", envelope.at("/payload/status").stringValue());
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
