package com.jobson.market.catalog_service.application;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

  public CategoryNotFoundException(UUID categoryId) {
    super("Category not found: " + categoryId);
  }
}
