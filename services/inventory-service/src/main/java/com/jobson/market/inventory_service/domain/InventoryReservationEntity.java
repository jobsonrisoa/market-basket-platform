package com.jobson.market.inventory_service.domain;

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
@Table(name = "inventory_reservations")
public class InventoryReservationEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID stockId;

  @Column(nullable = false)
  private UUID sellerId;

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false, precision = 19, scale = 3)
  private BigDecimal quantity;

  @Column(nullable = false)
  private String unit;

  @Column(nullable = false)
  private String requestedBy;

  @Column(nullable = false)
  private String referenceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InventoryReservationStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column private Instant releasedAt;

  protected InventoryReservationEntity() {}

  private InventoryReservationEntity(
      UUID id,
      UUID stockId,
      UUID sellerId,
      UUID productId,
      BigDecimal quantity,
      String unit,
      String requestedBy,
      String referenceId,
      InventoryReservationStatus status,
      Instant createdAt) {
    this.id = id;
    this.stockId = stockId;
    this.sellerId = sellerId;
    this.productId = productId;
    this.quantity = quantity;
    this.unit = unit;
    this.requestedBy = requestedBy;
    this.referenceId = referenceId;
    this.status = status;
    this.createdAt = createdAt;
  }

  public static InventoryReservationEntity active(
      InventoryStockEntity stock,
      BigDecimal quantity,
      String requestedBy,
      String referenceId,
      Instant createdAt) {
    if (stock == null) {
      throw new IllegalArgumentException("Stock is required");
    }
    BigDecimal requiredQuantity = requirePositive(quantity);
    String requiredRequestedBy = requireText(requestedBy, "Requested by is required");
    String requiredReferenceId = requireText(referenceId, "Reference id is required");
    if (createdAt == null) {
      throw new IllegalArgumentException("Created timestamp is required");
    }
    stock.reserve(requiredQuantity, createdAt);
    return new InventoryReservationEntity(
        UUID.randomUUID(),
        stock.id(),
        stock.sellerId(),
        stock.productId(),
        requiredQuantity,
        stock.unit(),
        requiredRequestedBy,
        requiredReferenceId,
        InventoryReservationStatus.ACTIVE,
        createdAt);
  }

  public void release(Instant releasedAt) {
    if (releasedAt == null) {
      throw new IllegalArgumentException("Released timestamp is required");
    }
    this.status = InventoryReservationStatus.RELEASED;
    this.releasedAt = releasedAt;
  }

  public UUID id() {
    return id;
  }

  public UUID stockId() {
    return stockId;
  }

  public UUID sellerId() {
    return sellerId;
  }

  public UUID productId() {
    return productId;
  }

  public BigDecimal quantity() {
    return quantity;
  }

  public String unit() {
    return unit;
  }

  public String requestedBy() {
    return requestedBy;
  }

  public String referenceId() {
    return referenceId;
  }

  public InventoryReservationStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant releasedAt() {
    return releasedAt;
  }

  private static BigDecimal requirePositive(BigDecimal quantity) {
    if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }
    return quantity;
  }

  private static String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }
}
