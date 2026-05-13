package com.jobson.market.inventory_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_stocks")
public class InventoryStockEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID sellerId;

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false, precision = 19, scale = 3)
  private BigDecimal onHandQuantity;

  @Column(nullable = false, precision = 19, scale = 3)
  private BigDecimal reservedQuantity;

  @Column(nullable = false)
  private String unit;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected InventoryStockEntity() {}

  private InventoryStockEntity(
      UUID id,
      UUID sellerId,
      UUID productId,
      BigDecimal onHandQuantity,
      BigDecimal reservedQuantity,
      String unit,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.sellerId = sellerId;
    this.productId = productId;
    this.onHandQuantity = onHandQuantity;
    this.reservedQuantity = reservedQuantity;
    this.unit = unit;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static InventoryStockEntity create(
      UUID sellerId, UUID productId, BigDecimal onHandQuantity, String unit, Instant now) {
    requireId(sellerId, "Seller id is required");
    requireId(productId, "Product id is required");
    BigDecimal requiredQuantity = requireNonNegative(onHandQuantity, "On-hand quantity");
    String requiredUnit = requireText(unit, "Unit is required");
    requireTimestamp(now, "Created timestamp is required");
    return new InventoryStockEntity(
        UUID.randomUUID(),
        sellerId,
        productId,
        requiredQuantity,
        BigDecimal.ZERO,
        requiredUnit,
        now,
        now);
  }

  public void replaceOnHand(BigDecimal onHandQuantity, String unit, Instant updatedAt) {
    BigDecimal requiredQuantity = requireNonNegative(onHandQuantity, "On-hand quantity");
    String requiredUnit = requireText(unit, "Unit is required");
    requireTimestamp(updatedAt, "Updated timestamp is required");
    if (requiredQuantity.compareTo(reservedQuantity) < 0) {
      throw new IllegalArgumentException("On-hand quantity cannot be below reserved quantity");
    }
    this.onHandQuantity = requiredQuantity;
    this.unit = requiredUnit;
    this.updatedAt = updatedAt;
  }

  public void reserve(BigDecimal quantity, Instant updatedAt) {
    BigDecimal requiredQuantity = requirePositive(quantity);
    requireTimestamp(updatedAt, "Updated timestamp is required");
    if (availableQuantity().compareTo(requiredQuantity) < 0) {
      throw new IllegalArgumentException("Insufficient available stock");
    }
    this.reservedQuantity = reservedQuantity.add(requiredQuantity);
    this.updatedAt = updatedAt;
  }

  public void release(BigDecimal quantity, Instant updatedAt) {
    BigDecimal requiredQuantity = requirePositive(quantity);
    requireTimestamp(updatedAt, "Updated timestamp is required");
    if (reservedQuantity.compareTo(requiredQuantity) < 0) {
      throw new IllegalArgumentException("Cannot release more than reserved quantity");
    }
    this.reservedQuantity = reservedQuantity.subtract(requiredQuantity);
    this.updatedAt = updatedAt;
  }

  public void commit(BigDecimal quantity, Instant updatedAt) {
    BigDecimal requiredQuantity = requirePositive(quantity);
    requireTimestamp(updatedAt, "Updated timestamp is required");
    if (reservedQuantity.compareTo(requiredQuantity) < 0) {
      throw new IllegalArgumentException("Cannot commit more than reserved quantity");
    }
    this.reservedQuantity = reservedQuantity.subtract(requiredQuantity);
    this.onHandQuantity = onHandQuantity.subtract(requiredQuantity);
    this.updatedAt = updatedAt;
  }

  public void adjustOnHand(BigDecimal quantityDelta, Instant updatedAt) {
    if (quantityDelta == null || quantityDelta.compareTo(BigDecimal.ZERO) == 0) {
      throw new IllegalArgumentException("Adjustment quantity delta is required");
    }
    requireTimestamp(updatedAt, "Updated timestamp is required");
    BigDecimal adjusted = onHandQuantity.add(quantityDelta);
    if (adjusted.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("On-hand quantity cannot be negative");
    }
    if (adjusted.compareTo(reservedQuantity) < 0) {
      throw new IllegalArgumentException("On-hand quantity cannot be below reserved quantity");
    }
    this.onHandQuantity = adjusted;
    this.updatedAt = updatedAt;
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

  public BigDecimal onHandQuantity() {
    return onHandQuantity;
  }

  public BigDecimal reservedQuantity() {
    return reservedQuantity;
  }

  public BigDecimal availableQuantity() {
    return onHandQuantity.subtract(reservedQuantity);
  }

  public String unit() {
    return unit;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  private static void requireId(UUID id, String message) {
    if (id == null) {
      throw new IllegalArgumentException(message);
    }
  }

  private static BigDecimal requireNonNegative(BigDecimal quantity, String label) {
    if (quantity == null) {
      throw new IllegalArgumentException(label + " is required");
    }
    if (quantity.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(label + " cannot be negative");
    }
    return quantity;
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

  private static void requireTimestamp(Instant value, String message) {
    if (value == null) {
      throw new IllegalArgumentException(message);
    }
  }
}
