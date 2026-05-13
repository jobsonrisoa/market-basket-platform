package com.jobson.market.order_service.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record DraftOrderDetails(
    UUID customerId,
    UUID sellerId,
    UUID productId,
    UUID stockId,
    BigDecimal quantity,
    String unit,
    String source,
    String sourceReferenceId) {}
