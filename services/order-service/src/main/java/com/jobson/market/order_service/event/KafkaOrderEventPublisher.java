package com.jobson.market.order_service.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class KafkaOrderEventPublisher implements OrderEventPublisher {

  private final KafkaTemplate<String, String> kafka;
  private final ObjectMapper objectMapper;

  KafkaOrderEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
    this.kafka = kafka;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(OrderEvent event) {
    try {
      kafka
          .send(
              event.eventType(), event.eventId().toString(), objectMapper.writeValueAsString(event))
          .get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while publishing order event", exception);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Failed to serialize order event", exception);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to publish order event", exception);
    }
  }
}
