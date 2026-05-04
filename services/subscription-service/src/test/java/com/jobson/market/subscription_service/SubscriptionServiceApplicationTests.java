package com.jobson.market.subscription_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SubscriptionServiceApplicationTests {

  @Test
  void contextLoads() {}
}
