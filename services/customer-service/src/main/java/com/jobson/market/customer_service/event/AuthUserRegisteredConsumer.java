package com.jobson.market.customer_service.event;

import com.jobson.market.customer_service.application.CustomerProfileService;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class AuthUserRegisteredConsumer {

  private final CustomerProfileService profiles;
  private final ObjectMapper objectMapper;

  public AuthUserRegisteredConsumer(CustomerProfileService profiles, ObjectMapper objectMapper) {
    this.profiles = profiles;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(
      topics = "${market.events.auth-user-registered-topic}",
      groupId = "${spring.kafka.consumer.group-id:${spring.application.name}}")
  public void handle(String event) {
    AuthUserRegisteredEvent registeredEvent = AuthUserRegisteredEvent.fromJson(event, objectMapper);
    profiles.ensureProfileForRegisteredUser(registeredEvent.authUserId());
  }

  private record AuthUserRegisteredEvent(UUID authUserId) {

    static AuthUserRegisteredEvent fromJson(String event, ObjectMapper objectMapper) {
      try {
        JsonNode root = objectMapper.readTree(event);
        if (!"auth.user.registered.v1".equals(root.path("eventType").stringValue())) {
          throw new IllegalArgumentException("Unsupported auth event type");
        }
        return new AuthUserRegisteredEvent(
            UUID.fromString(root.at("/payload/userId").stringValue()));
      } catch (RuntimeException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new IllegalArgumentException("Invalid auth user registered event", exception);
      }
    }
  }
}
