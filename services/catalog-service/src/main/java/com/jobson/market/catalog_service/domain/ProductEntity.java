package com.jobson.market.catalog_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "catalog_products")
public class ProductEntity {

  private static final String UPDATED_AT_REQUIRED = "updatedAt is required";

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false)
  private UUID sellerId;

  @Column(nullable = false)
  private UUID categoryId;

  @Column(nullable = false)
  private String name;

  @Column private String description;

  @Column(nullable = false)
  private String unit;

  @Column(nullable = false)
  private String packageSize;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal priceAmount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProductStatus status;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected ProductEntity() {}

  public static ProductEntity create(UUID sellerId, ProductDetails details, Instant now) {
    ProductEntity product = new ProductEntity();
    product.id = UUID.randomUUID();
    product.sellerId = Objects.requireNonNull(sellerId, "sellerId is required");
    product.status = ProductStatus.DRAFT;
    product.createdAt = Objects.requireNonNull(now, "createdAt is required");
    product.applyDetails(details);
    product.updatedAt = now;
    return product;
  }

  public void update(ProductDetails details, Instant now) {
    applyDetails(details);
    this.updatedAt = Objects.requireNonNull(now, UPDATED_AT_REQUIRED);
  }

  public void publish(Instant now) {
    this.status = ProductStatus.PUBLISHED;
    this.updatedAt = Objects.requireNonNull(now, UPDATED_AT_REQUIRED);
  }

  public void unpublish(Instant now) {
    this.status = ProductStatus.UNPUBLISHED;
    this.updatedAt = Objects.requireNonNull(now, UPDATED_AT_REQUIRED);
  }

  public UUID id() {
    return id;
  }

  public UUID sellerId() {
    return sellerId;
  }

  public UUID categoryId() {
    return categoryId;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public String unit() {
    return unit;
  }

  public String packageSize() {
    return packageSize;
  }

  public BigDecimal priceAmount() {
    return priceAmount;
  }

  public String currency() {
    return currency;
  }

  public ProductStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value.trim();
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static BigDecimal requirePrice(BigDecimal value) {
    Objects.requireNonNull(value, "priceAmount is required");
    if (value.signum() < 0) {
      throw new IllegalArgumentException("priceAmount must be non-negative");
    }
    return value;
  }

  private static String requireCurrency(String value) {
    String currency = requireText(value, "currency").toUpperCase();
    if (currency.length() != 3) {
      throw new IllegalArgumentException("currency must be a three-letter ISO code");
    }
    return currency;
  }

  private void applyDetails(ProductDetails details) {
    Objects.requireNonNull(details, "details is required");
    this.categoryId = Objects.requireNonNull(details.categoryId(), "categoryId is required");
    this.name = requireText(details.name(), "name");
    this.description = normalizeOptional(details.description());
    this.unit = requireText(details.unit(), "unit");
    this.packageSize = requireText(details.packageSize(), "packageSize");
    this.priceAmount = requirePrice(details.priceAmount());
    this.currency = requireCurrency(details.currency());
  }
}
