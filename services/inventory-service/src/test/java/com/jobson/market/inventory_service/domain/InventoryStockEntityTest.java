package com.jobson.market.inventory_service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryStockEntityTest {

  private static final UUID SELLER_ID = UUID.fromString("2f23681e-8e7f-4819-a7dd-ef4f88522921");
  private static final UUID PRODUCT_ID = UUID.fromString("7f3a7d58-e531-4ca8-918e-57c4663888c8");
  private static final Instant NOW = Instant.parse("2026-05-10T12:00:00Z");

  @Test
  void shouldCreateStockWithAvailability() {
    InventoryStockEntity stock =
        InventoryStockEntity.create(SELLER_ID, PRODUCT_ID, new BigDecimal("25.500"), "kg", NOW);

    assertEquals(SELLER_ID, stock.sellerId());
    assertEquals(PRODUCT_ID, stock.productId());
    assertEquals(new BigDecimal("25.500"), stock.onHandQuantity());
    assertEquals(BigDecimal.ZERO, stock.reservedQuantity());
    assertEquals(new BigDecimal("25.500"), stock.availableQuantity());
    assertEquals("kg", stock.unit());
  }

  @Test
  void shouldReserveAndReleaseStock() {
    InventoryStockEntity stock =
        InventoryStockEntity.create(SELLER_ID, PRODUCT_ID, new BigDecimal("10.00"), "kg", NOW);

    stock.reserve(new BigDecimal("3.50"), Instant.parse("2026-05-10T13:00:00Z"));
    stock.release(new BigDecimal("1.50"), Instant.parse("2026-05-10T14:00:00Z"));

    assertEquals(new BigDecimal("2.00"), stock.reservedQuantity());
    assertEquals(new BigDecimal("8.00"), stock.availableQuantity());
  }

  @Test
  void shouldRejectInvalidQuantities() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            InventoryStockEntity.create(SELLER_ID, PRODUCT_ID, new BigDecimal("-1.00"), "kg", NOW));

    InventoryStockEntity stock =
        InventoryStockEntity.create(SELLER_ID, PRODUCT_ID, new BigDecimal("2.00"), "kg", NOW);

    assertThrows(
        IllegalArgumentException.class,
        () -> stock.reserve(new BigDecimal("3.00"), Instant.parse("2026-05-10T13:00:00Z")));
    assertThrows(
        IllegalArgumentException.class,
        () -> stock.release(new BigDecimal("1.00"), Instant.parse("2026-05-10T13:00:00Z")));
  }
}
