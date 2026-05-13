package com.jobson.market.seller_service.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class KafkaSellerEventPublisher implements SellerEventPublisher {

  private final KafkaTemplate<String, String> kafka;
  private final ObjectMapper objectMapper;

  KafkaSellerEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
    this.kafka = kafka;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(SellerEvent event) {
    try {
      kafka
          .send(
              event.eventType(), event.eventId().toString(), objectMapper.writeValueAsString(event))
          .get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while publishing seller event", exception);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Failed to serialize seller event", exception);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to publish seller event", exception);
    }
  }
}
