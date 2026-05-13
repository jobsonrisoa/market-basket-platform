package com.jobson.market.catalog_service.application;

import java.util.Objects;
import java.util.UUID;

public class SellerNotApprovedException extends RuntimeException {

  public SellerNotApprovedException(UUID sellerId) {
    super("Seller must be approved before publishing products");
    Objects.requireNonNull(sellerId, "sellerId is required");
  }
}
