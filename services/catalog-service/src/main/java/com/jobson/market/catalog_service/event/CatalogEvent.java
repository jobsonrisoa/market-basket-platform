package com.jobson.market.catalog_service.event;

import com.jobson.market.catalog_service.domain.ProductEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CatalogEvent(
    UUID eventId,
    String eventType,
    int version,
    Instant occurredAt,
    String correlationId,
    ProductPublishedPayload payload) {

  public static CatalogEvent productPublished(ProductEntity product) {
    return new CatalogEvent(
        UUID.randomUUID(),
        "catalog.product.published.v1",
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        ProductPublishedPayload.from(product));
  }

  public record ProductPublishedPayload(
      String productId,
      String sellerId,
      String categoryId,
      String name,
      String unit,
      String packageSize,
      BigDecimal priceAmount,
      String currency,
      String status) {

    static ProductPublishedPayload from(ProductEntity product) {
      return new ProductPublishedPayload(
          product.id().toString(),
          product.sellerId().toString(),
          product.categoryId().toString(),
          product.name(),
          product.unit(),
          product.packageSize(),
          product.priceAmount(),
          product.currency(),
          product.status().name());
    }
  }
}
