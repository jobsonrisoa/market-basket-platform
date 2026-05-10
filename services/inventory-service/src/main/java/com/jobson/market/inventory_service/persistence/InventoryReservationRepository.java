package com.jobson.market.inventory_service.persistence;

import com.jobson.market.inventory_service.domain.InventoryReservationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository
    extends JpaRepository<InventoryReservationEntity, UUID> {}
