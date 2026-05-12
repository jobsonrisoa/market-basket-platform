package com.jobson.market.inventory_service.application;

import com.jobson.market.inventory_service.domain.InventoryReservationEntity;
import com.jobson.market.inventory_service.domain.InventoryReservationStatus;
import com.jobson.market.inventory_service.domain.InventoryStockEntity;
import com.jobson.market.inventory_service.persistence.InventoryReservationRepository;
import com.jobson.market.inventory_service.persistence.InventoryStockRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

  private final InventoryStockRepository stocks;
  private final InventoryReservationRepository reservations;
  private final Clock clock;

  public InventoryService(
      InventoryStockRepository stocks, InventoryReservationRepository reservations, Clock clock) {
    this.stocks = stocks;
    this.reservations = reservations;
    this.clock = clock;
  }

  @Transactional
  public InventoryStockEntity upsertStock(
      UUID sellerId, UUID productId, BigDecimal onHandQuantity, String unit) {
    return stocks
        .findBySellerIdAndProductId(sellerId, productId)
        .map(
            stock -> {
              stock.replaceOnHand(onHandQuantity, unit, clock.instant());
              return stocks.save(stock);
            })
        .orElseGet(
            () ->
                stocks.save(
                    InventoryStockEntity.create(
                        sellerId, productId, onHandQuantity, unit, clock.instant())));
  }

  @Transactional(readOnly = true)
  public InventoryStockEntity getStock(UUID stockId) {
    return requireStock(stockId);
  }

  @Transactional(readOnly = true)
  public List<InventoryStockEntity> listStocks(UUID sellerId) {
    return stocks.findBySellerIdOrderByCreatedAtAsc(sellerId);
  }

  @Transactional(readOnly = true)
  public InventoryReservationEntity getReservation(UUID reservationId) {
    return requireReservation(reservationId);
  }

  @Transactional
  public InventoryReservationEntity reserve(
      UUID stockId, BigDecimal quantity, String requestedBy, String referenceId) {
    InventoryStockEntity stock = requireStock(stockId);
    InventoryReservationEntity reservation =
        InventoryReservationEntity.active(
            stock, quantity, requestedBy, referenceId, clock.instant());
    stocks.save(stock);
    return reservations.save(reservation);
  }

  @Transactional
  public InventoryReservationEntity release(UUID reservationId) {
    InventoryReservationEntity reservation = requireReservation(reservationId);
    if (reservation.status() == InventoryReservationStatus.ACTIVE) {
      InventoryStockEntity stock = requireStock(reservation.stockId());
      reservation.release(clock.instant());
      stock.release(reservation.quantity(), clock.instant());
      stocks.save(stock);
      return reservations.save(reservation);
    }
    return reservation;
  }

  private InventoryStockEntity requireStock(UUID stockId) {
    return stocks
        .findById(stockId)
        .orElseThrow(() -> new InventoryNotFoundException("Inventory stock", stockId));
  }

  private InventoryReservationEntity requireReservation(UUID reservationId) {
    return reservations
        .findById(reservationId)
        .orElseThrow(() -> new InventoryNotFoundException("Inventory reservation", reservationId));
  }
}
