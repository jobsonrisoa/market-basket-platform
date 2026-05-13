package com.jobson.market.subscription_service.persistence;

import com.jobson.market.subscription_service.domain.SubscriptionEntity;
import com.jobson.market.subscription_service.domain.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface SubscriptionRepository extends Repository<SubscriptionEntity, UUID> {

  <S extends SubscriptionEntity> S save(S entity);

  Optional<SubscriptionEntity> findByIdAndCustomerId(UUID id, UUID customerId);

  List<SubscriptionEntity> findByCustomerIdOrderByCreatedAtAsc(UUID customerId);

  List<SubscriptionEntity> findByStatusAndNextRenewalDateLessThanEqual(
      SubscriptionStatus status, LocalDate renewalDate);

  void deleteAll();
}
