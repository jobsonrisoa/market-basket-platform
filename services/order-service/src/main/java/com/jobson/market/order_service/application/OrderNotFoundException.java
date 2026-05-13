package com.jobson.market.order_service.application;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

  public OrderNotFoundException(UUID orderId) {
    super("Order not found: " + orderId);
  }
}
