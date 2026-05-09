package com.jobson.market.auth.infrastructure.persistence;

import com.jobson.market.auth.application.port.event.OutboxEventRepository;
import com.jobson.market.auth.domain.event.OutboxEvent;
import org.springframework.stereotype.Repository;

@Repository
class JpaOutboxEventRepository implements OutboxEventRepository {

  private final SpringDataOutboxEventRepository events;

  JpaOutboxEventRepository(SpringDataOutboxEventRepository events) {
    this.events = events;
  }

  @Override
  public void save(OutboxEvent event) {
    events.save(OutboxEventEntity.pending(event));
  }
}
