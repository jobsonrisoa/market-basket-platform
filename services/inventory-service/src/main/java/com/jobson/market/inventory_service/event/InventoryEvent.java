package com.jobson.market.inventory_service.event;

import com.jobson.market.inventory_service.domain.InventoryReservationEntity;
import com.jobson.market.inventory_service.domain.InventoryStockEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryEvent(
    UUID eventId,
    String eventType,
    int version,
    Instant occurredAt,
    String correlationId,
    Object payload) {

  public static InventoryEvent stockReserved(
      InventoryStockEntity stock, InventoryReservationEntity reservation) {
    return event("inventory.stock_reserved.v1", StockReservedPayload.from(stock, reservation));
  }

  public static InventoryEvent reservationReleased(
      InventoryStockEntity stock, InventoryReservationEntity reservation) {
    return event(
        "inventory.reservation_released.v1", ReservationReleasedPayload.from(stock, reservation));
  }

  public static InventoryEvent reservationExpired(
      InventoryStockEntity stock, InventoryReservationEntity reservation) {
    return event(
        "inventory.reservation_expired.v1", ReservationExpiredPayload.from(stock, reservation));
  }

  public static InventoryEvent reservationCommitted(
      InventoryStockEntity stock, InventoryReservationEntity reservation) {
    return event(
        "inventory.reservation_committed.v1", ReservationCommittedPayload.from(stock, reservation));
  }

  public static InventoryEvent stockAdjusted(
      InventoryStockEntity stock, BigDecimal quantityDelta, String reason, String referenceId) {
    return event(
        "inventory.stock_adjusted.v1",
        new StockAdjustedPayload(
            stock.id().toString(),
            stock.sellerId().toString(),
            stock.productId().toString(),
            quantityDelta,
            stock.unit(),
            stock.onHandQuantity(),
            stock.availableQuantity(),
            reason,
            referenceId));
  }

  private static InventoryEvent event(String eventType, Object payload) {
    return new InventoryEvent(
        UUID.randomUUID(), eventType, 1, Instant.now(), UUID.randomUUID().toString(), payload);
  }

  public record StockReservedPayload(
      String stockId,
      String reservationId,
      String sellerId,
      String productId,
      BigDecimal quantity,
      String unit,
      BigDecimal availableQuantity,
      String requestedBy,
      String referenceId) {
    static StockReservedPayload from(
        InventoryStockEntity stock, InventoryReservationEntity reservation) {
      return new StockReservedPayload(
          stock.id().toString(),
          reservation.id().toString(),
          stock.sellerId().toString(),
          stock.productId().toString(),
          reservation.quantity(),
          stock.unit(),
          stock.availableQuantity(),
          reservation.requestedBy(),
          reservation.referenceId());
    }
  }

  public record ReservationReleasedPayload(
      String stockId,
      String reservationId,
      String sellerId,
      String productId,
      BigDecimal quantity,
      String unit,
      BigDecimal availableQuantity,
      String requestedBy,
      String referenceId,
      Instant releasedAt) {
    static ReservationReleasedPayload from(
        InventoryStockEntity stock, InventoryReservationEntity reservation) {
      return new ReservationReleasedPayload(
          stock.id().toString(),
          reservation.id().toString(),
          stock.sellerId().toString(),
          stock.productId().toString(),
          reservation.quantity(),
          stock.unit(),
          stock.availableQuantity(),
          reservation.requestedBy(),
          reservation.referenceId(),
          reservation.releasedAt());
    }
  }

  public record ReservationExpiredPayload(
      String stockId,
      String reservationId,
      String sellerId,
      String productId,
      BigDecimal quantity,
      String unit,
      BigDecimal availableQuantity,
      String requestedBy,
      String referenceId,
      Instant expiredAt) {
    static ReservationExpiredPayload from(
        InventoryStockEntity stock, InventoryReservationEntity reservation) {
      return new ReservationExpiredPayload(
          stock.id().toString(),
          reservation.id().toString(),
          stock.sellerId().toString(),
          stock.productId().toString(),
          reservation.quantity(),
          stock.unit(),
          stock.availableQuantity(),
          reservation.requestedBy(),
          reservation.referenceId(),
          reservation.expiredAt());
    }
  }

  public record ReservationCommittedPayload(
      String stockId,
      String reservationId,
      String sellerId,
      String productId,
      BigDecimal quantity,
      String unit,
      BigDecimal onHandQuantity,
      BigDecimal availableQuantity,
      String requestedBy,
      String referenceId,
      Instant committedAt) {
    static ReservationCommittedPayload from(
        InventoryStockEntity stock, InventoryReservationEntity reservation) {
      return new ReservationCommittedPayload(
          stock.id().toString(),
          reservation.id().toString(),
          stock.sellerId().toString(),
          stock.productId().toString(),
          reservation.quantity(),
          stock.unit(),
          stock.onHandQuantity(),
          stock.availableQuantity(),
          reservation.requestedBy(),
          reservation.referenceId(),
          reservation.committedAt());
    }
  }

  public record StockAdjustedPayload(
      String stockId,
      String sellerId,
      String productId,
      BigDecimal quantityDelta,
      String unit,
      BigDecimal onHandQuantity,
      BigDecimal availableQuantity,
      String reason,
      String referenceId) {}
}
