package com.jobson.market.catalog_service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductEntityTest {

  private static final UUID SELLER_ID = UUID.fromString("2f23681e-8e7f-4819-a7dd-ef4f88522921");
  private static final UUID CATEGORY_ID = UUID.fromString("7f3a7d58-e531-4ca8-918e-57c4663888c8");
  private static final Instant NOW = Instant.parse("2026-05-10T12:00:00Z");

  @Test
  void shouldCreateDraftProduct() {
    ProductEntity product =
        ProductEntity.create(
            SELLER_ID,
            CATEGORY_ID,
            " Organic Carrots ",
            "Fresh carrots",
            "kg",
            "1 kg bag",
            new BigDecimal("7.50"),
            "usd",
            NOW);

    assertEquals("Organic Carrots", product.name());
    assertEquals("Fresh carrots", product.description());
    assertEquals("kg", product.unit());
    assertEquals("1 kg bag", product.packageSize());
    assertEquals(new BigDecimal("7.50"), product.priceAmount());
    assertEquals("USD", product.currency());
    assertEquals(ProductStatus.DRAFT, product.status());
  }

  @Test
  void shouldRejectNegativePrice() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ProductEntity.create(
                SELLER_ID,
                CATEGORY_ID,
                "Organic Carrots",
                null,
                "kg",
                "1 kg bag",
                new BigDecimal("-1.00"),
                "USD",
                NOW));
  }

  @Test
  void shouldPublishAndUnpublishProduct() {
    ProductEntity product = product();
    Instant publishedAt = Instant.parse("2026-05-10T13:00:00Z");
    Instant unpublishedAt = Instant.parse("2026-05-10T14:00:00Z");

    product.publish(publishedAt);
    product.unpublish(unpublishedAt);

    assertEquals(ProductStatus.UNPUBLISHED, product.status());
    assertEquals(unpublishedAt, product.updatedAt());
  }

  @Test
  void shouldUpdateProductDetails() {
    ProductEntity product = product();
    Instant updatedAt = Instant.parse("2026-05-10T13:00:00Z");

    product.update(
        CATEGORY_ID,
        "Rainbow Carrots",
        "Colorful carrots",
        "bundle",
        "6 count",
        new BigDecimal("9.25"),
        "BRL",
        updatedAt);

    assertEquals("Rainbow Carrots", product.name());
    assertEquals("Colorful carrots", product.description());
    assertEquals("bundle", product.unit());
    assertEquals("6 count", product.packageSize());
    assertEquals(new BigDecimal("9.25"), product.priceAmount());
    assertEquals("BRL", product.currency());
    assertEquals(updatedAt, product.updatedAt());
  }

  private ProductEntity product() {
    return ProductEntity.create(
        SELLER_ID,
        CATEGORY_ID,
        "Organic Carrots",
        "Fresh carrots",
        "kg",
        "1 kg bag",
        new BigDecimal("7.50"),
        "USD",
        NOW);
  }
}
