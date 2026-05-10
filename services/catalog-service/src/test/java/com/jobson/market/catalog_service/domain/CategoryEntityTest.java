package com.jobson.market.catalog_service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CategoryEntityTest {

  @Test
  void shouldCreateCategoryWithTrimmedName() {
    Instant now = Instant.parse("2026-05-10T12:00:00Z");

    CategoryEntity category = CategoryEntity.create(" Produce ", now);

    assertEquals("Produce", category.name());
    assertEquals(now, category.createdAt());
    assertEquals(now, category.updatedAt());
  }

  @Test
  void shouldRejectBlankName() {
    Instant now = Instant.parse("2026-05-10T12:00:00Z");

    assertThrows(IllegalArgumentException.class, () -> CategoryEntity.create(" ", now));
  }

  @Test
  void shouldRenameCategory() {
    Instant createdAt = Instant.parse("2026-05-10T12:00:00Z");
    Instant updatedAt = Instant.parse("2026-05-10T13:00:00Z");
    CategoryEntity category = CategoryEntity.create("Produce", createdAt);

    category.rename(" Seasonal Produce ", updatedAt);

    assertEquals("Seasonal Produce", category.name());
    assertEquals(updatedAt, category.updatedAt());
  }
}
