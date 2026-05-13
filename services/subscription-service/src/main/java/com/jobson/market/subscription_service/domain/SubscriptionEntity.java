package com.jobson.market.subscription_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_subscriptions")
public class SubscriptionEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID customerId;

  @Column(nullable = false)
  private UUID planId;

  @Column(nullable = false)
  private UUID sellerId;

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false)
  private UUID stockId;

  @Column(nullable = false)
  private String basketSize;

  @Column(nullable = false)
  private String cadence;

  @Column(nullable = false, precision = 12, scale = 3)
  private BigDecimal quantity;

  @Column(nullable = false)
  private String unit;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionStatus status;

  @Column(nullable = false)
  private LocalDate nextRenewalDate;

  @Column(nullable = false)
  private UUID currentDraftOrderId;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  private Instant cancelledAt;

  protected SubscriptionEntity() {}

  private SubscriptionEntity(
      UUID customerId,
      SubscriptionPlanEntity plan,
      LocalDate nextRenewalDate,
      UUID currentDraftOrderId,
      Instant now) {
    this.id = UUID.randomUUID();
    this.customerId = customerId;
    this.planId = plan.id();
    this.sellerId = plan.sellerId();
    this.productId = plan.productId();
    this.stockId = plan.stockId();
    this.basketSize = plan.basketSize();
    this.cadence = plan.cadence();
    this.quantity = plan.quantity();
    this.unit = plan.unit();
    this.status = SubscriptionStatus.ACTIVE;
    this.nextRenewalDate = nextRenewalDate;
    this.currentDraftOrderId = currentDraftOrderId;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static SubscriptionEntity active(
      UUID customerId,
      SubscriptionPlanEntity plan,
      LocalDate nextRenewalDate,
      UUID currentDraftOrderId,
      Instant now) {
    return new SubscriptionEntity(customerId, plan, nextRenewalDate, currentDraftOrderId, now);
  }

  public void pause(Instant now) {
    if (status == SubscriptionStatus.ACTIVE) {
      status = SubscriptionStatus.PAUSED;
      updatedAt = now;
    }
  }

  public void resume(Instant now) {
    if (status == SubscriptionStatus.PAUSED) {
      status = SubscriptionStatus.ACTIVE;
      updatedAt = now;
    }
  }

  public void skip(Instant now) {
    nextRenewalDate = nextRenewalDate.plusDays(cadenceDays());
    updatedAt = now;
  }

  public void renew(UUID draftOrderId, Instant now) {
    currentDraftOrderId = draftOrderId;
    nextRenewalDate = nextRenewalDate.plusDays(cadenceDays());
    updatedAt = now;
  }

  public void cancel(Instant now) {
    status = SubscriptionStatus.CANCELLED;
    cancelledAt = now;
    updatedAt = now;
  }

  private int cadenceDays() {
    return "BIWEEKLY".equalsIgnoreCase(cadence) ? 14 : 7;
  }

  public UUID id() {
    return id;
  }

  public UUID customerId() {
    return customerId;
  }

  public UUID planId() {
    return planId;
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

  public String basketSize() {
    return basketSize;
  }

  public String cadence() {
    return cadence;
  }

  public BigDecimal quantity() {
    return quantity;
  }

  public String unit() {
    return unit;
  }

  public SubscriptionStatus status() {
    return status;
  }

  public LocalDate nextRenewalDate() {
    return nextRenewalDate;
  }

  public UUID currentDraftOrderId() {
    return currentDraftOrderId;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public Instant cancelledAt() {
    return cancelledAt;
  }
}
