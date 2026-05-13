package com.jobson.market.subscription_service.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionPlanDetails(
    UUID sellerId,
    UUID productId,
    UUID stockId,
    String basketSize,
    String cadence,
    BigDecimal quantity,
    String unit) {}
