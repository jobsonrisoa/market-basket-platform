package com.jobson.market.subscription_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jobson.market.subscription_service.domain.SubscriptionEntity;
import com.jobson.market.subscription_service.domain.SubscriptionStatus;
import com.jobson.market.subscription_service.event.SubscriptionEventPublisher;
import com.jobson.market.subscription_service.persistence.SubscriptionPlanRepository;
import com.jobson.market.subscription_service.persistence.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionServiceTest {

  private final InMemoryPlanRepository plans = new InMemoryPlanRepository();
  private final InMemorySubscriptionRepository subscriptions = new InMemorySubscriptionRepository();
  private final RecordingSubscriptionEventPublisher events =
      new RecordingSubscriptionEventPublisher();
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-13T12:00:00Z"), ZoneOffset.UTC);
  private final SubscriptionService service =
      new SubscriptionService(plans, subscriptions, events, clock);

  @Test
  void shouldCreateActiveSubscriptionWithGeneratedDraftOrder() {
    UUID customerId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID stockId = UUID.randomUUID();

    SubscriptionEntity subscription =
        service.createSubscription(
            new CreateSubscriptionCommand(
                customerId,
                sellerId,
                UUID.randomUUID(),
                stockId,
                "SMALL",
                "WEEKLY",
                new BigDecimal("3.0"),
                "kg",
                LocalDate.parse("2026-05-20")));

    assertEquals(SubscriptionStatus.ACTIVE, subscription.status());
    assertEquals(LocalDate.parse("2026-05-20"), subscription.nextRenewalDate());
    assertEquals(1, subscriptions.count());
    assertEquals("subscription.renewal_due.v1", events.eventTypes().get(0));
  }

  @Test
  void shouldPauseResumeSkipAndCancelSubscription() {
    SubscriptionEntity subscription = sampleSubscription();

    assertEquals(
        SubscriptionStatus.PAUSED,
        service.pause(subscription.id(), subscription.customerId()).status());
    assertEquals(
        SubscriptionStatus.ACTIVE,
        service.resume(subscription.id(), subscription.customerId()).status());
    assertEquals(
        LocalDate.parse("2026-05-27"),
        service.skip(subscription.id(), subscription.customerId()).nextRenewalDate());
    assertEquals(
        SubscriptionStatus.CANCELLED,
        service.cancel(subscription.id(), subscription.customerId()).status());
  }

  @Test
  void shouldEmitRenewalDueForDueActiveSubscriptions() {
    sampleSubscription();
    events.clear();

    List<SubscriptionEntity> renewed = service.renewDueSubscriptions(LocalDate.parse("2026-05-20"));

    assertEquals(1, renewed.size());
    assertEquals(LocalDate.parse("2026-05-27"), renewed.get(0).nextRenewalDate());
    assertEquals("subscription.renewal_due.v1", events.eventTypes().get(0));
  }

  private SubscriptionEntity sampleSubscription() {
    return service.createSubscription(
        new CreateSubscriptionCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "SMALL",
            "WEEKLY",
            BigDecimal.ONE,
            "kg",
            LocalDate.parse("2026-05-20")));
  }

  private static final class RecordingSubscriptionEventPublisher
      implements SubscriptionEventPublisher {
    private final List<String> eventTypes = new ArrayList<>();

    @Override
    public void publish(com.jobson.market.subscription_service.event.SubscriptionEvent event) {
      eventTypes.add(event.eventType());
    }

    List<String> eventTypes() {
      return eventTypes;
    }

    void clear() {
      eventTypes.clear();
    }
  }

  private static final class InMemoryPlanRepository implements SubscriptionPlanRepository {
    private final List<com.jobson.market.subscription_service.domain.SubscriptionPlanEntity>
        stored = new ArrayList<>();

    @Override
    public <S extends com.jobson.market.subscription_service.domain.SubscriptionPlanEntity> S save(
        S entity) {
      stored.add(entity);
      return entity;
    }

    @Override
    public void deleteAll() {
      stored.clear();
    }
  }

  private static final class InMemorySubscriptionRepository implements SubscriptionRepository {
    private final List<SubscriptionEntity> stored = new ArrayList<>();

    @Override
    public <S extends SubscriptionEntity> S save(S entity) {
      stored.removeIf(existing -> existing.id().equals(entity.id()));
      stored.add(entity);
      return entity;
    }

    @Override
    public Optional<SubscriptionEntity> findByIdAndCustomerId(UUID id, UUID customerId) {
      return stored.stream()
          .filter(subscription -> subscription.id().equals(id))
          .filter(subscription -> subscription.customerId().equals(customerId))
          .findFirst();
    }

    @Override
    public List<SubscriptionEntity> findByCustomerIdOrderByCreatedAtAsc(UUID customerId) {
      return stored.stream()
          .filter(subscription -> subscription.customerId().equals(customerId))
          .toList();
    }

    @Override
    public List<SubscriptionEntity> findByStatusAndNextRenewalDateLessThanEqual(
        SubscriptionStatus status, LocalDate renewalDate) {
      return stored.stream()
          .filter(subscription -> subscription.status() == status)
          .filter(subscription -> !subscription.nextRenewalDate().isAfter(renewalDate))
          .toList();
    }

    long count() {
      return stored.size();
    }

    @Override
    public void deleteAll() {
      stored.clear();
    }
  }
}
