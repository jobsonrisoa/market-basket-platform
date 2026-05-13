package com.jobson.market.order_service.application;

import java.math.BigDecimal;
import java.util.UUID;

public interface InventoryReservationClient {

  void reserve(UUID stockId, BigDecimal quantity, String referenceId);

  void release(UUID stockId, String referenceId);
}
