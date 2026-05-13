package com.jobson.market.order_service.domain;

public enum OrderFulfillmentStatus {
  DRAFT,
  CONFIRMED,
  FULFILLMENT_READY,
  FULFILLED,
  FAILED
}
