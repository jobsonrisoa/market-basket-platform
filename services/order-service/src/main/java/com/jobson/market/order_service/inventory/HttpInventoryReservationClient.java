package com.jobson.market.order_service.inventory;

import com.jobson.market.order_service.application.InventoryReservationClient;
import com.jobson.market.order_service.application.InventoryReservationFailedException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class HttpInventoryReservationClient implements InventoryReservationClient {

  private final RestClient restClient;

  HttpInventoryReservationClient(
      @Value("${market.inventory.base-url:http://localhost:8082}") String inventoryBaseUrl) {
    this.restClient = RestClient.create(inventoryBaseUrl);
  }

  @Override
  public void reserve(UUID stockId, BigDecimal quantity, String referenceId) {
    try {
      restClient
          .post()
          .uri("/inventory/reservations")
          .body(new ReservationRequest(stockId, quantity, "order-service", referenceId))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException exception) {
      throw new InventoryReservationFailedException("Inventory reservation failed", exception);
    }
  }

  @Override
  public void release(UUID stockId, String referenceId) {
    restClient
        .post()
        .uri("/inventory/reservations/release")
        .body(new ReservationCommand(stockId, "order-service", referenceId))
        .retrieve()
        .toBodilessEntity();
  }

  private record ReservationRequest(
      UUID stockId, BigDecimal quantity, String requestedBy, String referenceId) {}

  private record ReservationCommand(UUID stockId, String requestedBy, String referenceId) {}
}
