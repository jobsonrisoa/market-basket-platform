package com.jobson.market.inventory_service.web;

import com.jobson.market.inventory_service.application.InventoryService;
import com.jobson.market.inventory_service.domain.InventoryReservationEntity;
import com.jobson.market.inventory_service.domain.InventoryReservationStatus;
import com.jobson.market.inventory_service.domain.InventoryStockEntity;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
class InventoryController {

  private final InventoryService inventory;

  InventoryController(InventoryService inventory) {
    this.inventory = inventory;
  }

  @PostMapping("/stocks")
  ResponseEntity<InventoryStockResponse> upsertStock(
      @Valid @RequestBody UpsertStockRequest request, Authentication authentication) {
    AuthenticatedSellerAccess.requireSellerAccess(authentication, request.sellerId());
    InventoryStockEntity stock =
        inventory.upsertStock(
            request.sellerId(), request.productId(), request.onHandQuantity(), request.unit());
    return ResponseEntity.status(HttpStatus.CREATED).body(InventoryStockResponse.from(stock));
  }

  @GetMapping("/stocks/{stockId}")
  InventoryStockResponse getStock(@PathVariable UUID stockId, Authentication authentication) {
    InventoryStockEntity stock = inventory.getStock(stockId);
    AuthenticatedSellerAccess.requireSellerAccess(authentication, stock.sellerId());
    return InventoryStockResponse.from(stock);
  }

  @GetMapping("/stocks")
  List<InventoryStockResponse> listStocks(
      @RequestParam UUID sellerId, Authentication authentication) {
    AuthenticatedSellerAccess.requireSellerAccess(authentication, sellerId);
    return inventory.listStocks(sellerId).stream().map(InventoryStockResponse::from).toList();
  }

  @PostMapping("/reservations")
  ResponseEntity<InventoryReservationResponse> reserve(
      @Valid @RequestBody CreateReservationRequest request, Authentication authentication) {
    InventoryStockEntity stock = inventory.getStock(request.stockId());
    AuthenticatedSellerAccess.requireSellerAccess(authentication, stock.sellerId());
    InventoryReservationEntity reservation =
        inventory.reserve(
            request.stockId(), request.quantity(), request.requestedBy(), request.referenceId());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(InventoryReservationResponse.from(reservation));
  }

  @PostMapping("/reservations/{reservationId}/release")
  InventoryReservationResponse release(
      @PathVariable UUID reservationId, Authentication authentication) {
    InventoryReservationEntity reservation = inventory.getReservation(reservationId);
    AuthenticatedSellerAccess.requireSellerAccess(authentication, reservation.sellerId());
    return InventoryReservationResponse.from(inventory.release(reservationId));
  }

  record UpsertStockRequest(
      @NotNull UUID sellerId,
      @NotNull UUID productId,
      @NotNull @Positive BigDecimal onHandQuantity,
      @NotBlank String unit) {}

  record CreateReservationRequest(
      @NotNull UUID stockId,
      @NotNull @Positive BigDecimal quantity,
      @NotBlank String requestedBy,
      @NotBlank String referenceId) {}

  record InventoryStockResponse(
      UUID id,
      UUID sellerId,
      UUID productId,
      BigDecimal onHandQuantity,
      BigDecimal reservedQuantity,
      BigDecimal availableQuantity,
      String unit,
      Instant createdAt,
      Instant updatedAt) {
    static InventoryStockResponse from(InventoryStockEntity stock) {
      return new InventoryStockResponse(
          stock.id(),
          stock.sellerId(),
          stock.productId(),
          stock.onHandQuantity(),
          stock.reservedQuantity(),
          stock.availableQuantity(),
          stock.unit(),
          stock.createdAt(),
          stock.updatedAt());
    }
  }

  record InventoryReservationResponse(
      UUID id,
      UUID stockId,
      UUID sellerId,
      UUID productId,
      BigDecimal quantity,
      String unit,
      String requestedBy,
      String referenceId,
      InventoryReservationStatus status,
      Instant createdAt,
      Instant releasedAt) {
    static InventoryReservationResponse from(InventoryReservationEntity reservation) {
      return new InventoryReservationResponse(
          reservation.id(),
          reservation.stockId(),
          reservation.sellerId(),
          reservation.productId(),
          reservation.quantity(),
          reservation.unit(),
          reservation.requestedBy(),
          reservation.referenceId(),
          reservation.status(),
          reservation.createdAt(),
          reservation.releasedAt());
    }
  }
}
