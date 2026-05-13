package com.jobson.market.subscription_service.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateSubscriptionCommand(
    UUID customerId,
    UUID sellerId,
    UUID productId,
    UUID stockId,
    String basketSize,
    String cadence,
    BigDecimal quantity,
    String unit,
    LocalDate nextRenewalDate) {}
