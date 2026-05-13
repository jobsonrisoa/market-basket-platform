package com.jobson.market.subscription_service.web;

import com.jobson.market.subscription_service.application.CreateSubscriptionCommand;
import com.jobson.market.subscription_service.application.SubscriptionService;
import com.jobson.market.subscription_service.domain.SubscriptionEntity;
import com.jobson.market.subscription_service.domain.SubscriptionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscriptions")
class SubscriptionController {

  private final SubscriptionService subscriptions;

  SubscriptionController(SubscriptionService subscriptions) {
    this.subscriptions = subscriptions;
  }

  @PostMapping
  ResponseEntity<SubscriptionResponse> create(
      @Valid @RequestBody CreateSubscriptionRequest request) {
    SubscriptionEntity subscription =
        subscriptions.createSubscription(
            new CreateSubscriptionCommand(
                request.customerId(),
                request.sellerId(),
                request.productId(),
                request.stockId(),
                request.basketSize(),
                request.cadence(),
                request.quantity(),
                request.unit(),
                request.nextRenewalDate()));
    return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.from(subscription));
  }

  @GetMapping
  List<SubscriptionResponse> list(@RequestParam UUID customerId) {
    return subscriptions.listCustomerSubscriptions(customerId).stream()
        .map(SubscriptionResponse::from)
        .toList();
  }

  @PostMapping("/{subscriptionId}/pause")
  SubscriptionResponse pause(@PathVariable UUID subscriptionId, @RequestParam UUID customerId) {
    return SubscriptionResponse.from(subscriptions.pause(subscriptionId, customerId));
  }

  @PostMapping("/{subscriptionId}/resume")
  SubscriptionResponse resume(@PathVariable UUID subscriptionId, @RequestParam UUID customerId) {
    return SubscriptionResponse.from(subscriptions.resume(subscriptionId, customerId));
  }

  @PostMapping("/{subscriptionId}/skip")
  SubscriptionResponse skip(@PathVariable UUID subscriptionId, @RequestParam UUID customerId) {
    return SubscriptionResponse.from(subscriptions.skip(subscriptionId, customerId));
  }

  @PostMapping("/{subscriptionId}/cancel")
  SubscriptionResponse cancel(@PathVariable UUID subscriptionId, @RequestParam UUID customerId) {
    return SubscriptionResponse.from(subscriptions.cancel(subscriptionId, customerId));
  }

  @PostMapping("/renewals/due")
  List<SubscriptionResponse> renewDue(@RequestParam LocalDate renewalDate) {
    return subscriptions.renewDueSubscriptions(renewalDate).stream()
        .map(SubscriptionResponse::from)
        .toList();
  }

  record CreateSubscriptionRequest(
      @NotNull UUID customerId,
      @NotNull UUID sellerId,
      @NotNull UUID productId,
      @NotNull UUID stockId,
      @NotBlank String basketSize,
      @NotBlank String cadence,
      @NotNull @Positive BigDecimal quantity,
      @NotBlank String unit,
      @NotNull LocalDate nextRenewalDate) {}

  record SubscriptionResponse(
      UUID id,
      UUID customerId,
      UUID planId,
      UUID sellerId,
      UUID productId,
      UUID stockId,
      String basketSize,
      String cadence,
      BigDecimal quantity,
      String unit,
      SubscriptionStatus status,
      LocalDate nextRenewalDate,
      UUID currentDraftOrderId,
      Instant createdAt,
      Instant updatedAt,
      Instant cancelledAt) {
    static SubscriptionResponse from(SubscriptionEntity subscription) {
      return new SubscriptionResponse(
          subscription.id(),
          subscription.customerId(),
          subscription.planId(),
          subscription.sellerId(),
          subscription.productId(),
          subscription.stockId(),
          subscription.basketSize(),
          subscription.cadence(),
          subscription.quantity(),
          subscription.unit(),
          subscription.status(),
          subscription.nextRenewalDate(),
          subscription.currentDraftOrderId(),
          subscription.createdAt(),
          subscription.updatedAt(),
          subscription.cancelledAt());
    }
  }
}
