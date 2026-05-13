package com.jobson.market.subscription_service.persistence;

import com.jobson.market.subscription_service.domain.SubscriptionPlanEntity;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface SubscriptionPlanRepository extends Repository<SubscriptionPlanEntity, UUID> {

  <S extends SubscriptionPlanEntity> S save(S entity);

  void deleteAll();
}
