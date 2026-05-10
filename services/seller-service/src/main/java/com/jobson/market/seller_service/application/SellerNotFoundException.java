package com.jobson.market.seller_service.application;

import java.util.UUID;

public class SellerNotFoundException extends RuntimeException {

  public SellerNotFoundException(UUID sellerId) {
    super("Seller not found: " + sellerId);
  }
}
