package com.jobson.market.subscription_service.event;

public interface SubscriptionEventPublisher {

  void publish(SubscriptionEvent event);
}
