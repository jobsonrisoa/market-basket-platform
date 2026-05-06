package com.jobson.market.auth.infrastructure.persistence;

import java.time.Clock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class KafkaOutboxPublisher {

  private final SpringDataOutboxEventRepository events;
  private final KafkaTemplate<String, String> kafka;
  private final Clock clock;

  KafkaOutboxPublisher(
      SpringDataOutboxEventRepository events, KafkaTemplate<String, String> kafka, Clock clock) {
    this.events = events;
    this.kafka = kafka;
    this.clock = clock;
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

  private String envelope(OutboxEventEntity event) {
    return """
        {"eventId":"%s","eventType":"%s","version":%d,"occurredAt":"%s","correlationId":"%s","payload":%s}\
        """
        .formatted(
            event.eventId(),
            event.eventType(),
            event.version(),
            event.occurredAt(),
            event.correlationId(),
            event.payload());
  }
}
