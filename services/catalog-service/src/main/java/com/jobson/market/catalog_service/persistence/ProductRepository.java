package com.jobson.market.catalog_service.persistence;

import com.jobson.market.catalog_service.domain.ProductEntity;
import com.jobson.market.catalog_service.domain.ProductStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

  List<ProductEntity> findBySellerIdOrderByCreatedAtAsc(UUID sellerId);

  List<ProductEntity> findBySellerIdAndStatusOrderByCreatedAtAsc(
      UUID sellerId, ProductStatus status);
}
