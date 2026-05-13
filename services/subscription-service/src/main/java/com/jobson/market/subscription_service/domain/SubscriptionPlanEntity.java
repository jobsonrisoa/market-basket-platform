package com.jobson.market.subscription_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlanEntity {

  @Id private UUID id;

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

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected SubscriptionPlanEntity() {}

  private SubscriptionPlanEntity(SubscriptionPlanDetails details, Instant now) {
    this.id = UUID.randomUUID();
    this.sellerId = details.sellerId();
    this.productId = details.productId();
    this.stockId = details.stockId();
    this.basketSize = details.basketSize().trim();
    this.cadence = details.cadence().trim();
    this.quantity = details.quantity();
    this.unit = details.unit().trim();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static SubscriptionPlanEntity create(SubscriptionPlanDetails details, Instant now) {
    return new SubscriptionPlanEntity(details, now);
  }

  public UUID id() {
    return id;
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
}
