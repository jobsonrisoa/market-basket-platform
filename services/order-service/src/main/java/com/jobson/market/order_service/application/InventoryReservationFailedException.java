package com.jobson.market.order_service.application;

public class InventoryReservationFailedException extends RuntimeException {

  public InventoryReservationFailedException(String message) {
    super(message);
  }

  public InventoryReservationFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
