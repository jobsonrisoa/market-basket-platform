package com.jobson.market.inventory_service.application;

import java.util.UUID;

public class InventoryNotFoundException extends RuntimeException {
  public InventoryNotFoundException(String resource, UUID id) {
    super(resource + " not found: " + id);
  }
}
