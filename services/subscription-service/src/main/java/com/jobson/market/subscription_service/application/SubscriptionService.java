package com.jobson.market.subscription_service.application;

import com.jobson.market.subscription_service.domain.SubscriptionEntity;
import com.jobson.market.subscription_service.domain.SubscriptionPlanDetails;
import com.jobson.market.subscription_service.domain.SubscriptionPlanEntity;
import com.jobson.market.subscription_service.domain.SubscriptionStatus;
import com.jobson.market.subscription_service.event.SubscriptionEvent;
import com.jobson.market.subscription_service.event.SubscriptionEventPublisher;
import com.jobson.market.subscription_service.persistence.SubscriptionPlanRepository;
import com.jobson.market.subscription_service.persistence.SubscriptionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

  private final SubscriptionPlanRepository plans;
  private final SubscriptionRepository subscriptions;
  private final SubscriptionEventPublisher events;
  private final Clock clock;

  public SubscriptionService(
      SubscriptionPlanRepository plans,
      SubscriptionRepository subscriptions,
      SubscriptionEventPublisher events,
      Clock clock) {
    this.plans = plans;
    this.subscriptions = subscriptions;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public SubscriptionEntity createSubscription(CreateSubscriptionCommand command) {
    SubscriptionPlanEntity plan =
        plans.save(
            SubscriptionPlanEntity.create(
                new SubscriptionPlanDetails(
                    command.sellerId(),
                    command.productId(),
                    command.stockId(),
                    command.basketSize(),
                    command.cadence(),
                    command.quantity(),
                    command.unit()),
                clock.instant()));
    SubscriptionEntity subscription =
        subscriptions.save(
            SubscriptionEntity.active(
                command.customerId(),
                plan,
                command.nextRenewalDate(),
                UUID.randomUUID(),
                clock.instant()));
    events.publish(SubscriptionEvent.renewalDue(subscription));
    return subscription;
  }

  @Transactional(readOnly = true)
  public List<SubscriptionEntity> listCustomerSubscriptions(UUID customerId) {
    return subscriptions.findByCustomerIdOrderByCreatedAtAsc(customerId);
  }

  @Transactional
  public SubscriptionEntity pause(UUID subscriptionId, UUID customerId) {
    SubscriptionEntity subscription = requireSubscription(subscriptionId, customerId);
    subscription.pause(clock.instant());
    return subscriptions.save(subscription);
  }

  @Transactional
  public SubscriptionEntity resume(UUID subscriptionId, UUID customerId) {
    SubscriptionEntity subscription = requireSubscription(subscriptionId, customerId);
    subscription.resume(clock.instant());
    return subscriptions.save(subscription);
  }

  @Transactional
  public SubscriptionEntity skip(UUID subscriptionId, UUID customerId) {
    SubscriptionEntity subscription = requireSubscription(subscriptionId, customerId);
    subscription.skip(clock.instant());
    return subscriptions.save(subscription);
  }

  @Transactional
  public SubscriptionEntity cancel(UUID subscriptionId, UUID customerId) {
    SubscriptionEntity subscription = requireSubscription(subscriptionId, customerId);
    subscription.cancel(clock.instant());
    return subscriptions.save(subscription);
  }

  @Transactional
  public List<SubscriptionEntity> renewDueSubscriptions(LocalDate renewalDate) {
    return subscriptions
        .findByStatusAndNextRenewalDateLessThanEqual(SubscriptionStatus.ACTIVE, renewalDate)
        .stream()
        .map(this::renew)
        .toList();
  }

  private SubscriptionEntity renew(SubscriptionEntity subscription) {
    subscription.renew(UUID.randomUUID(), clock.instant());
    SubscriptionEntity saved = subscriptions.save(subscription);
    events.publish(SubscriptionEvent.renewalDue(saved));
    return saved;
  }

  private SubscriptionEntity requireSubscription(UUID subscriptionId, UUID customerId) {
    return subscriptions
        .findByIdAndCustomerId(subscriptionId, customerId)
        .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
  }
}
