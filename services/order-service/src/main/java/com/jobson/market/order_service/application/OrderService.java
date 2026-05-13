package com.jobson.market.order_service.application;

import com.jobson.market.order_service.domain.DraftOrderDetails;
import com.jobson.market.order_service.domain.OrderEntity;
import com.jobson.market.order_service.domain.OrderFulfillmentStatus;
import com.jobson.market.order_service.domain.OrderStatus;
import com.jobson.market.order_service.event.OrderEvent;
import com.jobson.market.order_service.event.OrderEventPublisher;
import com.jobson.market.order_service.persistence.OrderRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  private final OrderRepository orders;
  private final InventoryReservationClient inventory;
  private final OrderEventPublisher events;
  private final Clock clock;

  public OrderService(
      OrderRepository orders,
      InventoryReservationClient inventory,
      OrderEventPublisher events,
      Clock clock) {
    this.orders = orders;
    this.inventory = inventory;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public OrderEntity createDraftOrder(CreateDraftOrderCommand command) {
    return orders.save(
        OrderEntity.draft(
            new DraftOrderDetails(
                command.customerId(),
                command.sellerId(),
                command.productId(),
                command.stockId(),
                command.quantity(),
                command.unit(),
                command.source(),
                command.sourceReferenceId()),
            clock.instant()));
  }

  @Transactional(readOnly = true)
  public OrderEntity getOrder(UUID orderId) {
    return requireOrder(orderId);
  }

  @Transactional(readOnly = true)
  public List<OrderEntity> listCustomerOrders(UUID customerId) {
    return orders.findByCustomerIdOrderByCreatedAtAsc(customerId);
  }

  @Transactional
  public OrderEntity confirm(UUID orderId) {
    OrderEntity order = requireOrder(orderId);
    try {
      inventory.reserve(order.stockId(), order.quantity(), order.id().toString());
      order.confirm(clock.instant());
      OrderEntity saved = orders.save(order);
      events.publish(OrderEvent.confirmed(saved));
      return saved;
    } catch (InventoryReservationFailedException exception) {
      order.fail(clock.instant());
      orders.save(order);
      throw exception;
    }
  }

  @Transactional
  public OrderEntity cancel(UUID orderId) {
    OrderEntity order = requireOrder(orderId);
    OrderStatus previousStatus = order.status();
    order.cancel(clock.instant());
    OrderEntity saved = orders.save(order);
    if (previousStatus == OrderStatus.CONFIRMED
        || previousStatus == OrderStatus.FULFILLMENT_READY) {
      inventory.release(saved.stockId(), saved.id().toString());
    }
    return saved;
  }

  @Transactional
  public OrderEntity changeFulfillmentStatus(UUID orderId, OrderFulfillmentStatus status) {
    OrderEntity order = requireOrder(orderId);
    order.changeFulfillmentStatus(status, clock.instant());
    OrderEntity saved = orders.save(order);
    events.publish(OrderEvent.fulfillmentStatusChanged(saved));
    return saved;
  }

  private OrderEntity requireOrder(UUID orderId) {
    return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
  }
}
