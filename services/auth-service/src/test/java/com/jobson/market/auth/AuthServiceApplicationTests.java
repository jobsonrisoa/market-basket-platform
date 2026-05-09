package com.jobson.market.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuthServiceApplicationTests {

  @Test
  void contextLoads() {
    // Spring Boot performs the assertion by starting the application context.
  }
}
