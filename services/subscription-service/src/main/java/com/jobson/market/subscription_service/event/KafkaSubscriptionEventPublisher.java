package com.jobson.market.subscription_service.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class KafkaSubscriptionEventPublisher implements SubscriptionEventPublisher {

  private final KafkaTemplate<String, String> kafka;
  private final ObjectMapper objectMapper;

  KafkaSubscriptionEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
    this.kafka = kafka;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(SubscriptionEvent event) {
    try {
      kafka
          .send(
              event.eventType(), event.eventId().toString(), objectMapper.writeValueAsString(event))
          .get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while publishing subscription event", exception);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Failed to serialize subscription event", exception);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to publish subscription event", exception);
    }
  }
}
