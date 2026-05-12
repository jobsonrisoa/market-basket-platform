package com.jobson.market.customer_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jobson.market.customer_service.TestcontainersConfiguration;
import com.jobson.market.customer_service.persistence.CustomerProfileRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuthUserRegisteredConsumerContractTest {

  private static final Path EXAMPLE =
      Path.of("../auth-service/src/test/resources/contracts/auth/examples/user-registered-v1.json");

  @Autowired private AuthUserRegisteredConsumer consumer;
  @Autowired private CustomerProfileRepository profiles;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void cleanDatabase() {
    profiles.deleteAll();
  }

  @Test
  void shouldCreateInitialProfileFromRecordedAuthRegisteredExampleIdempotently() throws Exception {
    String event = Files.readString(EXAMPLE);
    UUID authUserId =
        UUID.fromString(objectMapper.readTree(event).at("/payload/userId").stringValue());

    consumer.handle(event);
    consumer.handle(event);

    assertEquals(1, profiles.count());
    assertEquals(authUserId, profiles.findByAuthUserId(authUserId).orElseThrow().authUserId());
  }
}
