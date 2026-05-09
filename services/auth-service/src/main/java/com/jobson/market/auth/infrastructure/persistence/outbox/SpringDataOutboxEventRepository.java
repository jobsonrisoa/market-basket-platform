package com.jobson.market.auth.infrastructure.persistence.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

  List<OutboxEventEntity> findTop50ByStatusOrderByOccurredAtAsc(String status);
}
