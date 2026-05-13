package com.jobson.market.order_service.web;

import com.jobson.market.order_service.application.CreateDraftOrderCommand;
import com.jobson.market.order_service.application.OrderService;
import com.jobson.market.order_service.domain.OrderEntity;
import com.jobson.market.order_service.domain.OrderFulfillmentStatus;
import com.jobson.market.order_service.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
class OrderController {

  private final OrderService orders;

  OrderController(OrderService orders) {
    this.orders = orders;
  }

  @PostMapping
  ResponseEntity<OrderResponse> createDraft(@Valid @RequestBody CreateOrderRequest request) {
    OrderEntity order =
        orders.createDraftOrder(
            new CreateDraftOrderCommand(
                request.customerId(),
                request.sellerId(),
                request.productId(),
                request.stockId(),
                request.quantity(),
                request.unit(),
                request.source(),
                request.sourceReferenceId()));
    return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
  }

  @GetMapping("/{orderId}")
  OrderResponse getOrder(@PathVariable UUID orderId) {
    return OrderResponse.from(orders.getOrder(orderId));
  }

  @GetMapping
  List<OrderResponse> listCustomerOrders(@RequestParam UUID customerId) {
    return orders.listCustomerOrders(customerId).stream().map(OrderResponse::from).toList();
  }

  @PostMapping("/{orderId}/confirm")
  OrderResponse confirm(@PathVariable UUID orderId) {
    return OrderResponse.from(orders.confirm(orderId));
  }

  @PostMapping("/{orderId}/cancel")
  OrderResponse cancel(@PathVariable UUID orderId) {
    return OrderResponse.from(orders.cancel(orderId));
  }

  @PatchMapping("/{orderId}/fulfillment")
  OrderResponse changeFulfillment(
      @PathVariable UUID orderId, @Valid @RequestBody FulfillmentRequest request) {
    return OrderResponse.from(orders.changeFulfillmentStatus(orderId, request.status()));
  }

  record CreateOrderRequest(
      @NotNull UUID customerId,
      @NotNull UUID sellerId,
      @NotNull UUID productId,
      @NotNull UUID stockId,
      @NotNull @Positive BigDecimal quantity,
      @NotBlank String unit,
      @NotBlank String source,
      @NotBlank String sourceReferenceId) {}

  record FulfillmentRequest(@NotNull OrderFulfillmentStatus status) {}

  record OrderResponse(
      UUID id,
      UUID customerId,
      UUID sellerId,
      UUID productId,
      UUID stockId,
      BigDecimal quantity,
      String unit,
      String source,
      String sourceReferenceId,
      OrderStatus status,
      OrderFulfillmentStatus fulfillmentStatus,
      Instant createdAt,
      Instant updatedAt,
      Instant confirmedAt,
      Instant cancelledAt,
      Instant fulfilledAt) {
    static OrderResponse from(OrderEntity order) {
      return new OrderResponse(
          order.id(),
          order.customerId(),
          order.sellerId(),
          order.productId(),
          order.stockId(),
          order.quantity(),
          order.unit(),
          order.source(),
          order.sourceReferenceId(),
          order.status(),
          order.fulfillmentStatus(),
          order.createdAt(),
          order.updatedAt(),
          order.confirmedAt(),
          order.cancelledAt(),
          order.fulfilledAt());
    }
  }
}
