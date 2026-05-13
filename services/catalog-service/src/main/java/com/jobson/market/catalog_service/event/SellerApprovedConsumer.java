package com.jobson.market.catalog_service.event;

import com.jobson.market.catalog_service.domain.SellerApprovalState;
import com.jobson.market.catalog_service.domain.SellerApprovalStatus;
import com.jobson.market.catalog_service.persistence.SellerApprovalRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class SellerApprovedConsumer {

  private final SellerApprovalRepository sellerApprovals;
  private final ObjectMapper objectMapper;

  public SellerApprovedConsumer(
      SellerApprovalRepository sellerApprovals, ObjectMapper objectMapper) {
    this.sellerApprovals = sellerApprovals;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(
      topics = "${market.events.seller-approved-topic}",
      groupId = "${spring.kafka.consumer.group-id:${spring.application.name}}")
  @Transactional
  public void handle(String event) {
    SellerReviewEvent approvedEvent =
        SellerReviewEvent.fromJson(event, objectMapper, "seller.approved.v1");
    recordReview(approvedEvent, SellerApprovalStatus.APPROVED);
  }

  @KafkaListener(
      topics = "${market.events.seller-rejected-topic}",
      groupId = "${spring.kafka.consumer.group-id:${spring.application.name}}")
  @Transactional
  public void handleRejected(String event) {
    SellerReviewEvent rejectedEvent =
        SellerReviewEvent.fromJson(event, objectMapper, "seller.rejected.v1");
    recordReview(rejectedEvent, SellerApprovalStatus.REJECTED);
  }

  private void recordReview(SellerReviewEvent reviewEvent, SellerApprovalStatus status) {
    sellerApprovals
        .findBySellerId(reviewEvent.sellerId())
        .ifPresentOrElse(
            state ->
                state.recordReview(
                    reviewEvent.sellerName(),
                    status,
                    reviewEvent.reviewedByUserId(),
                    reviewEvent.reviewedAt()),
            () ->
                sellerApprovals.save(
                    SellerApprovalState.reviewed(
                        reviewEvent.sellerId(),
                        reviewEvent.sellerName(),
                        status,
                        reviewEvent.reviewedByUserId(),
                        reviewEvent.reviewedAt())));
  }

  private record SellerReviewEvent(
      UUID sellerId, String sellerName, UUID reviewedByUserId, Instant reviewedAt) {

    static SellerReviewEvent fromJson(
        String event, ObjectMapper objectMapper, String expectedEventType) {
      try {
        JsonNode root = objectMapper.readTree(event);
        if (!expectedEventType.equals(root.path("eventType").stringValue())) {
          throw new IllegalArgumentException("Unsupported seller event type");
        }
        JsonNode payload = root.path("payload");
        return new SellerReviewEvent(
            UUID.fromString(payload.path("sellerId").stringValue()),
            payload.path("name").stringValue(),
            UUID.fromString(payload.path("reviewedByUserId").stringValue()),
            Instant.parse(payload.path("reviewedAt").stringValue()));
      } catch (RuntimeException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new IllegalArgumentException("Invalid seller review event", exception);
      }
    }
  }
}
