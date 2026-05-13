package com.jobson.market.order_service.domain;

public enum OrderStatus {
  DRAFT,
  CONFIRMED,
  CANCELLED,
  FULFILLMENT_READY,
  FULFILLED,
  FAILED
}
