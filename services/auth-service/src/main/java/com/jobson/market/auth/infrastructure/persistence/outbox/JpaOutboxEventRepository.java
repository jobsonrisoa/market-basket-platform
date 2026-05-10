package com.jobson.market.auth.infrastructure.persistence.outbox;

import com.jobson.market.auth.application.port.event.OutboxEventRepository;
import com.jobson.market.auth.domain.event.OutboxEvent;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
class JpaOutboxEventRepository implements OutboxEventRepository {

  private final SpringDataOutboxEventRepository events;
  private final ObjectMapper objectMapper;

  JpaOutboxEventRepository(SpringDataOutboxEventRepository events, ObjectMapper objectMapper) {
    this.events = events;
    this.objectMapper = objectMapper;
  }

  @Override
  public void save(OutboxEvent event) {
    events.save(OutboxEventEntity.pending(event, payloadJson(event)));
  }

  private String payloadJson(OutboxEvent event) {
    try {
      return objectMapper.writeValueAsString(event.payload());
    } catch (JacksonException exception) {
      throw new IllegalStateException("Invalid outbox event payload", exception);
    }
  }
}
