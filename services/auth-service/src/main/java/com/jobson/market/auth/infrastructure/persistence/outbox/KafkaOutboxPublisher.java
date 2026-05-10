package com.jobson.market.auth.infrastructure.persistence.outbox;

import java.time.Clock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
class KafkaOutboxPublisher {

  private final SpringDataOutboxEventRepository events;
  private final KafkaTemplate<String, String> kafka;
  private final Clock clock;
  private final ObjectMapper objectMapper;

  KafkaOutboxPublisher(
      SpringDataOutboxEventRepository events,
      KafkaTemplate<String, String> kafka,
      Clock clock,
      ObjectMapper objectMapper) {
    this.events = events;
    this.kafka = kafka;
    this.clock = clock;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelayString = "${auth.outbox.publish-delay:PT5S}")
  @Transactional
  void publishPendingEvents() {
    for (OutboxEventEntity event : events.findTop50ByStatusOrderByOccurredAtAsc("PENDING")) {
      try {
        kafka.send(event.eventType(), event.eventId().toString(), envelope(event)).get();
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while publishing outbox event", exception);
      } catch (Exception exception) {
        throw new IllegalStateException("Failed to publish outbox event", exception);
      }
      event.markPublished(clock.instant());
      events.save(event);
    }
  }

  String envelope(OutboxEventEntity event) {
    ObjectNode envelope = objectMapper.createObjectNode();
    envelope.put("eventId", event.eventId().toString());
    envelope.put("eventType", event.eventType());
    envelope.put("version", event.version());
    envelope.put("occurredAt", event.occurredAt().toString());
    envelope.put("correlationId", event.correlationId());
    try {
      envelope.set("payload", objectMapper.readTree(event.payload()));
      return objectMapper.writeValueAsString(envelope);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Invalid outbox event payload JSON", exception);
    }
  }
}
