package com.jobson.market.subscription_service;

import org.springframework.boot.SpringApplication;

public class TestSubscriptionServiceApplication {

  public static void main(String[] args) {
    SpringApplication.from(SubscriptionServiceApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
