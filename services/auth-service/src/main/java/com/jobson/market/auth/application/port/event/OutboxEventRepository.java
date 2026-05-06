package com.jobson.market.auth.application.port.event;

import com.jobson.market.auth.domain.event.OutboxEvent;

public interface OutboxEventRepository {

  void save(OutboxEvent event);
}
