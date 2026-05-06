package com.jobson.market.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
class OutboxEventEntity {

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

  @Lob
  @Column(nullable = false)
  private String payload;

  @Column(nullable = false)
  private String status;

  @Column private Instant publishedAt;

  protected OutboxEventEntity() {}

  OutboxEventEntity(
      UUID eventId,
      String aggregateId,
      String eventType,
      int version,
      Instant occurredAt,
      String correlationId,
      String payload,
      String status) {
    this.eventId = eventId;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.version = version;
    this.occurredAt = occurredAt;
    this.correlationId = correlationId;
    this.payload = payload;
    this.status = status;
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

  void markPublished(Instant publishedAt) {
    this.status = "PUBLISHED";
    this.publishedAt = publishedAt;
  }
}
