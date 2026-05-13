package com.jobson.market.order_service.event;

import com.jobson.market.order_service.domain.OrderEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
    UUID eventId,
    String eventType,
    int version,
    Instant occurredAt,
    String correlationId,
    OrderPayload payload) {

  public static OrderEvent confirmed(OrderEntity order) {
    return new OrderEvent(
        UUID.randomUUID(),
        "order.confirmed.v1",
        1,
        Instant.now(),
        order.id().toString(),
        OrderPayload.from(order));
  }

  public static OrderEvent fulfillmentStatusChanged(OrderEntity order) {
    return new OrderEvent(
        UUID.randomUUID(),
        "order.fulfillment_status_changed.v1",
        1,
        Instant.now(),
        order.id().toString(),
        OrderPayload.from(order));
  }

  public record OrderPayload(
      String orderId,
      String customerId,
      String sellerId,
      String productId,
      String stockId,
      BigDecimal quantity,
      String unit,
      String status,
      String fulfillmentStatus,
      Instant confirmedAt,
      Instant fulfilledAt) {

    static OrderPayload from(OrderEntity order) {
      return new OrderPayload(
          order.id().toString(),
          order.customerId().toString(),
          order.sellerId().toString(),
          order.productId().toString(),
          order.stockId().toString(),
          order.quantity(),
          order.unit(),
          order.status().name(),
          order.fulfillmentStatus().name(),
          order.confirmedAt(),
          order.fulfilledAt());
    }
  }
}
