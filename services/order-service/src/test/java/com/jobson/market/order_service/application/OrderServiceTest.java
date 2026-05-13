package com.jobson.market.order_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jobson.market.order_service.domain.OrderEntity;
import com.jobson.market.order_service.domain.OrderFulfillmentStatus;
import com.jobson.market.order_service.domain.OrderStatus;
import com.jobson.market.order_service.event.OrderEventPublisher;
import com.jobson.market.order_service.persistence.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

  private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
  private final RecordingInventoryClient inventory = new RecordingInventoryClient();
  private final RecordingOrderEventPublisher events = new RecordingOrderEventPublisher();
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-13T12:00:00Z"), ZoneOffset.UTC);
  private final OrderService service = new OrderService(orders, inventory, events, clock);

  @BeforeEach
  void reset() {
    orders.clear();
    inventory.reset();
    events.clear();
  }

  @Test
  void shouldCreateDraftAndConfirmWithInventoryReservation() {
    UUID customerId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    UUID stockId = UUID.randomUUID();

    OrderEntity draft =
        service.createDraftOrder(
            new CreateDraftOrderCommand(
                customerId,
                sellerId,
                productId,
                stockId,
                new BigDecimal("2.5"),
                "kg",
                "subscription-service",
                "sub-123"));

    OrderEntity confirmed = service.confirm(draft.id());

    assertEquals(OrderStatus.CONFIRMED, confirmed.status());
    assertEquals(1, inventory.reserveCalls);
    assertEquals(confirmed.id().toString(), inventory.lastReferenceId);
    assertEquals("order.confirmed.v1", events.publishedTypes().get(0));
  }

  @Test
  void shouldReleaseReservationWhenCancellingConfirmedOrder() {
    OrderEntity confirmed = service.confirm(sampleDraft().id());

    OrderEntity cancelled = service.cancel(confirmed.id());

    assertEquals(OrderStatus.CANCELLED, cancelled.status());
    assertEquals(1, inventory.releaseCalls);
  }

  @Test
  void shouldPersistAndPublishFulfillmentTransitions() {
    OrderEntity confirmed = service.confirm(sampleDraft().id());

    OrderEntity ready =
        service.changeFulfillmentStatus(confirmed.id(), OrderFulfillmentStatus.FULFILLMENT_READY);
    assertEquals(OrderStatus.FULFILLMENT_READY, ready.status());

    OrderEntity fulfilled =
        service.changeFulfillmentStatus(confirmed.id(), OrderFulfillmentStatus.FULFILLED);

    assertEquals(OrderStatus.FULFILLED, fulfilled.status());
    assertEquals(3, events.publishedTypes().size());
    assertEquals("order.fulfillment_status_changed.v1", events.publishedTypes().get(1));
  }

  @Test
  void shouldRejectConfirmationWhenInventoryReservationFails() {
    OrderEntity draft = sampleDraft();
    inventory.failReserve = true;

    assertThrows(InventoryReservationFailedException.class, () -> confirm(draft));
    assertEquals(OrderStatus.FAILED, orders.findById(draft.id()).orElseThrow().status());
  }

  private OrderEntity sampleDraft() {
    return service.createDraftOrder(
        new CreateDraftOrderCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.ONE,
            "kg",
            "subscription-service",
            "sub-123"));
  }

  private void confirm(OrderEntity draft) {
    service.confirm(draft.id());
  }

  private static final class RecordingInventoryClient implements InventoryReservationClient {
    private int reserveCalls;
    private int releaseCalls;
    private boolean failReserve;
    private String lastReferenceId;

    @Override
    public void reserve(UUID stockId, BigDecimal quantity, String referenceId) {
      reserveCalls++;
      lastReferenceId = referenceId;
      if (failReserve) {
        throw new InventoryReservationFailedException("Inventory unavailable");
      }
    }

    @Override
    public void release(UUID stockId, String referenceId) {
      releaseCalls++;
    }

    void reset() {
      reserveCalls = 0;
      releaseCalls = 0;
      failReserve = false;
      lastReferenceId = null;
    }
  }

  private static final class RecordingOrderEventPublisher implements OrderEventPublisher {
    private final List<String> eventTypes = new ArrayList<>();

    @Override
    public void publish(com.jobson.market.order_service.event.OrderEvent event) {
      eventTypes.add(event.eventType());
    }

    List<String> publishedTypes() {
      return eventTypes;
    }

    void clear() {
      eventTypes.clear();
    }
  }

  private static final class InMemoryOrderRepository implements OrderRepository {
    private final List<OrderEntity> stored = new ArrayList<>();

    @Override
    public <S extends OrderEntity> S save(S entity) {
      deleteById(entity.id());
      stored.add(entity);
      return entity;
    }

    @Override
    public Optional<OrderEntity> findById(UUID id) {
      return stored.stream().filter(order -> order.id().equals(id)).findFirst();
    }

    @Override
    public List<OrderEntity> findByCustomerIdOrderByCreatedAtAsc(UUID customerId) {
      return stored.stream().filter(order -> order.customerId().equals(customerId)).toList();
    }

    void deleteById(UUID id) {
      stored.removeIf(order -> order.id().equals(id));
    }

    void clear() {
      stored.clear();
    }

    @Override
    public void deleteAll() {
      clear();
    }
  }
}
