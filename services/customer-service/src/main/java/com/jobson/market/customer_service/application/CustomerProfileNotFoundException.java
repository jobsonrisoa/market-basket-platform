package com.jobson.market.customer_service.application;

import java.util.UUID;

public class CustomerProfileNotFoundException extends RuntimeException {

  public CustomerProfileNotFoundException(UUID authUserId) {
    super("Customer profile not found for auth user " + authUserId);
  }
}
