package com.jobson.market.order_service.event;

public interface OrderEventPublisher {

  void publish(OrderEvent event);
}
