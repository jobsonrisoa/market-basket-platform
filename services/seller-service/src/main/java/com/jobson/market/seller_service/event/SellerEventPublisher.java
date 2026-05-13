package com.jobson.market.seller_service.event;

public interface SellerEventPublisher {

  void publish(SellerEvent event);
}
