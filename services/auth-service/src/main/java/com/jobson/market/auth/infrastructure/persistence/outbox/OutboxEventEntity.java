package com.jobson.market.auth.infrastructure.persistence.outbox;

import com.jobson.market.auth.domain.event.OutboxEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
class OutboxEventEntity {

  private static final String PENDING_STATUS = "PENDING";
  private static final String PUBLISHED_STATUS = "PUBLISHED";

  @Id private UUID eventId;

  @Column(nullable = false)
  private String aggregateId;

  @Column(nullable = false)
  private String eventType;

  @Column(nullable = false)
  private int version;

  @Column(nullable = false)
  private Instant occurredAt;

  @Column(nullable = false)
  private String correlationId;

  @Column(nullable = false, columnDefinition = "text")
  private String payload;

  @Column(nullable = false)
  private String status;

  @Column private Instant publishedAt;

  protected OutboxEventEntity() {}

  static OutboxEventEntity pending(OutboxEvent event, String payloadJson) {
    OutboxEventEntity entity = new OutboxEventEntity();
    entity.eventId = event.eventId();
    entity.aggregateId = event.aggregateId();
    entity.eventType = event.eventType();
    entity.version = event.version();
    entity.occurredAt = event.occurredAt();
    entity.correlationId = event.correlationId();
    entity.payload = payloadJson;
    entity.status = PENDING_STATUS;
    return entity;
  }

  UUID eventId() {
    return eventId;
  }

  String eventType() {
    return eventType;
  }

  int version() {
    return version;
  }

  Instant occurredAt() {
    return occurredAt;
  }

  String correlationId() {
    return correlationId;
  }

  String payload() {
    return payload;
  }

  String status() {
    return status;
  }

  void markPublished(Instant publishedAt) {
    this.status = PUBLISHED_STATUS;
    this.publishedAt = publishedAt;
  }
}
