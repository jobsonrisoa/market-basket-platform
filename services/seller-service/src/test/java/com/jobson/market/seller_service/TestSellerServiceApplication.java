package com.jobson.market.seller_service;

import org.springframework.boot.SpringApplication;

public class TestSellerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.from(SellerServiceApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
