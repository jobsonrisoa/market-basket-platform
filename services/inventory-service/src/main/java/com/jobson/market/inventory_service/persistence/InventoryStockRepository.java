package com.jobson.market.inventory_service.persistence;

import com.jobson.market.inventory_service.domain.InventoryStockEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryStockRepository extends JpaRepository<InventoryStockEntity, UUID> {
  List<InventoryStockEntity> findBySellerIdOrderByCreatedAtAsc(UUID sellerId);

  Optional<InventoryStockEntity> findBySellerIdAndProductId(UUID sellerId, UUID productId);
}
