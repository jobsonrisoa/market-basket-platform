package com.jobson.market.auth.application.port;

import com.jobson.market.auth.domain.event.OutboxEvent;

public interface OutboxEventRepository {

  void save(OutboxEvent event);
}
