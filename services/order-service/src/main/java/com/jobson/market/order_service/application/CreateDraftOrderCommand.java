package com.jobson.market.order_service.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateDraftOrderCommand(
    UUID customerId,
    UUID sellerId,
    UUID productId,
    UUID stockId,
    BigDecimal quantity,
    String unit,
    String source,
    String sourceReferenceId) {}
