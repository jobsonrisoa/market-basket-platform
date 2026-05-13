package com.jobson.market.subscription_service.application;

import java.util.UUID;

public class SubscriptionNotFoundException extends RuntimeException {

  public SubscriptionNotFoundException(UUID subscriptionId) {
    super("Subscription not found: " + subscriptionId);
  }
}
