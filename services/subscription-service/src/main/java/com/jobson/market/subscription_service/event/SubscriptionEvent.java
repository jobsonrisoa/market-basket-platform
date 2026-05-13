package com.jobson.market.subscription_service.event;

import com.jobson.market.subscription_service.domain.SubscriptionEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionEvent(
    UUID eventId,
    String eventType,
    int version,
    Instant occurredAt,
    String correlationId,
    RenewalDuePayload payload) {

  public static SubscriptionEvent renewalDue(SubscriptionEntity subscription) {
    return new SubscriptionEvent(
        UUID.randomUUID(),
        "subscription.renewal_due.v1",
        1,
        Instant.now(),
        subscription.id().toString(),
        RenewalDuePayload.from(subscription));
  }

  public record RenewalDuePayload(
      String subscriptionId,
      String customerId,
      String sellerId,
      String productId,
      String stockId,
      String draftOrderId,
      String basketSize,
      String cadence,
      BigDecimal quantity,
      String unit,
      LocalDate nextRenewalDate,
      String status) {

    static RenewalDuePayload from(SubscriptionEntity subscription) {
      return new RenewalDuePayload(
          subscription.id().toString(),
          subscription.customerId().toString(),
          subscription.sellerId().toString(),
          subscription.productId().toString(),
          subscription.stockId().toString(),
          subscription.currentDraftOrderId().toString(),
          subscription.basketSize(),
          subscription.cadence(),
          subscription.quantity(),
          subscription.unit(),
          subscription.nextRenewalDate(),
          subscription.status().name());
    }
  }
}
