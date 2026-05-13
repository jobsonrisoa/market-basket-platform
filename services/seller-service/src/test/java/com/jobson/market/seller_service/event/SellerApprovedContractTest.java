package com.jobson.market.seller_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.seller_service.domain.SellerApprovalStatus;
import com.jobson.market.seller_service.domain.SellerStoreEntity;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SellerApprovedContractTest {

  private static final UUID OWNER_USER_ID = UUID.fromString("7fd9b32c-c557-4ab9-9650-8f85122225f8");
  private static final UUID REVIEWER_USER_ID =
      UUID.fromString("b4b2ee03-eef2-4e7e-a80f-136cbfc21aa8");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSerializeSellerApprovedEnvelopeThatMatchesConsumerContract() throws Exception {
    SellerStoreEntity seller =
        SellerStoreEntity.create(
            "Fresh Market", OWNER_USER_ID, Instant.parse("2026-05-10T12:00:00Z"));
    seller.approve(
        REVIEWER_USER_ID, "Ready for marketplace", Instant.parse("2026-05-10T13:00:00Z"));

    SellerEvent event = SellerEvent.sellerApproved(seller);
    JsonNode envelope = objectMapper.valueToTree(event);
    JsonNode schema =
        objectMapper.readTree(
            getClass().getResourceAsStream("/contracts/seller/seller-approved-v1.schema.json"));

    assertEquals("seller.approved.v1", event.eventType());
    assertEquals(schema.path("title").stringValue(), envelope.path("eventType").stringValue());
    assertEquals(
        schema.at("/properties/eventType/const").stringValue(),
        envelope.path("eventType").stringValue());
    assertEquals(schema.at("/properties/version/const").asInt(), envelope.path("version").asInt());
    assertObjectHasOnlyFields(
        envelope, "eventId", "eventType", "version", "occurredAt", "correlationId", "payload");
    assertObjectHasOnlyFields(
        envelope.path("payload"),
        "sellerId",
        "name",
        "ownerUserId",
        "approvalStatus",
        "reviewedByUserId",
        "reviewedAt");
    assertRequiredFieldsExist(envelope, schema.path("required"));
    assertRequiredFieldsExist(envelope.path("payload"), schema.at("/properties/payload/required"));
    UUID.fromString(envelope.path("eventId").stringValue());
    assertFalse(envelope.path("correlationId").stringValue().isBlank());
    assertEquals(seller.id().toString(), envelope.at("/payload/sellerId").stringValue());
    assertEquals(OWNER_USER_ID.toString(), envelope.at("/payload/ownerUserId").stringValue());
    assertEquals(
        REVIEWER_USER_ID.toString(), envelope.at("/payload/reviewedByUserId").stringValue());
    assertEquals(
        SellerApprovalStatus.APPROVED.name(), envelope.at("/payload/approvalStatus").stringValue());
  }

  @Test
  void shouldSerializeSellerRejectedEnvelopeThatMatchesConsumerContract() throws Exception {
    SellerStoreEntity seller =
        SellerStoreEntity.create(
            "Fresh Market", OWNER_USER_ID, Instant.parse("2026-05-10T12:00:00Z"));
    seller.reject(REVIEWER_USER_ID, "Needs documents", Instant.parse("2026-05-10T14:00:00Z"));

    SellerEvent event = SellerEvent.sellerRejected(seller);
    JsonNode envelope = objectMapper.valueToTree(event);
    JsonNode schema =
        objectMapper.readTree(
            getClass().getResourceAsStream("/contracts/seller/seller-rejected-v1.schema.json"));

    assertEquals("seller.rejected.v1", event.eventType());
    assertEquals(schema.path("title").stringValue(), envelope.path("eventType").stringValue());
    assertEquals(
        schema.at("/properties/eventType/const").stringValue(),
        envelope.path("eventType").stringValue());
    assertEquals(schema.at("/properties/version/const").asInt(), envelope.path("version").asInt());
    assertObjectHasOnlyFields(
        envelope, "eventId", "eventType", "version", "occurredAt", "correlationId", "payload");
    assertObjectHasOnlyFields(
        envelope.path("payload"),
        "sellerId",
        "name",
        "ownerUserId",
        "approvalStatus",
        "reviewedByUserId",
        "reviewedAt");
    assertRequiredFieldsExist(envelope, schema.path("required"));
    assertRequiredFieldsExist(envelope.path("payload"), schema.at("/properties/payload/required"));
    UUID.fromString(envelope.path("eventId").stringValue());
    assertFalse(envelope.path("correlationId").stringValue().isBlank());
    assertEquals(seller.id().toString(), envelope.at("/payload/sellerId").stringValue());
    assertEquals(OWNER_USER_ID.toString(), envelope.at("/payload/ownerUserId").stringValue());
    assertEquals(
        REVIEWER_USER_ID.toString(), envelope.at("/payload/reviewedByUserId").stringValue());
    assertEquals(
        SellerApprovalStatus.REJECTED.name(), envelope.at("/payload/approvalStatus").stringValue());
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
