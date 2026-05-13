package com.jobson.market.order_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID customerId;

  @Column(nullable = false)
  private UUID sellerId;

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false)
  private UUID stockId;

  @Column(nullable = false, precision = 12, scale = 3)
  private BigDecimal quantity;

  @Column(nullable = false)
  private String unit;

  @Column(nullable = false)
  private String source;

  @Column(nullable = false)
  private String sourceReferenceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderFulfillmentStatus fulfillmentStatus;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  private Instant confirmedAt;
  private Instant cancelledAt;
  private Instant fulfilledAt;

  protected OrderEntity() {}

  private OrderEntity(UUID id, DraftOrderDetails details, Instant now) {
    this.id = id;
    this.customerId = details.customerId();
    this.sellerId = details.sellerId();
    this.productId = details.productId();
    this.stockId = details.stockId();
    this.quantity = details.quantity();
    this.unit = details.unit().trim();
    this.source = details.source().trim();
    this.sourceReferenceId = details.sourceReferenceId().trim();
    this.status = OrderStatus.DRAFT;
    this.fulfillmentStatus = OrderFulfillmentStatus.DRAFT;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static OrderEntity draft(DraftOrderDetails details, Instant now) {
    return new OrderEntity(UUID.randomUUID(), details, now);
  }

  public void confirm(Instant now) {
    if (status == OrderStatus.DRAFT || status == OrderStatus.FAILED) {
      status = OrderStatus.CONFIRMED;
      fulfillmentStatus = OrderFulfillmentStatus.CONFIRMED;
      confirmedAt = now;
      updatedAt = now;
    }
  }

  public void fail(Instant now) {
    status = OrderStatus.FAILED;
    fulfillmentStatus = OrderFulfillmentStatus.FAILED;
    updatedAt = now;
  }

  public void cancel(Instant now) {
    if (status != OrderStatus.FULFILLED && status != OrderStatus.CANCELLED) {
      status = OrderStatus.CANCELLED;
      cancelledAt = now;
      updatedAt = now;
    }
  }

  public void changeFulfillmentStatus(OrderFulfillmentStatus nextStatus, Instant now) {
    fulfillmentStatus = nextStatus;
    if (nextStatus == OrderFulfillmentStatus.FULFILLMENT_READY) {
      status = OrderStatus.FULFILLMENT_READY;
    } else if (nextStatus == OrderFulfillmentStatus.FULFILLED) {
      status = OrderStatus.FULFILLED;
      fulfilledAt = now;
    } else if (nextStatus == OrderFulfillmentStatus.FAILED) {
      status = OrderStatus.FAILED;
    }
    updatedAt = now;
  }

  public UUID id() {
    return id;
  }

  public UUID customerId() {
    return customerId;
  }

  public UUID sellerId() {
    return sellerId;
  }

  public UUID productId() {
    return productId;
  }

  public UUID stockId() {
    return stockId;
  }

  public BigDecimal quantity() {
    return quantity;
  }

  public String unit() {
    return unit;
  }

  public String source() {
    return source;
  }

  public String sourceReferenceId() {
    return sourceReferenceId;
  }

  public OrderStatus status() {
    return status;
  }

  public OrderFulfillmentStatus fulfillmentStatus() {
    return fulfillmentStatus;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public Instant confirmedAt() {
    return confirmedAt;
  }

  public Instant cancelledAt() {
    return cancelledAt;
  }

  public Instant fulfilledAt() {
    return fulfilledAt;
  }
}
