package com.jobson.market.catalog_service.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDetails(
    UUID categoryId,
    String name,
    String description,
    String unit,
    String packageSize,
    BigDecimal priceAmount,
    String currency) {}
