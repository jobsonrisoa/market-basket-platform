package com.jobson.market.inventory_service.persistence;

import com.jobson.market.inventory_service.domain.InventoryReservationEntity;
import com.jobson.market.inventory_service.domain.InventoryReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository
    extends JpaRepository<InventoryReservationEntity, UUID> {

  Optional<InventoryReservationEntity> findByStockIdAndRequestedByAndReferenceId(
      UUID stockId, String requestedBy, String referenceId);

  List<InventoryReservationEntity> findByStatusAndExpiresAtLessThanEqual(
      InventoryReservationStatus status, Instant expiresAt);
}
