package com.jobson.market.seller_service.persistence;

import com.jobson.market.seller_service.domain.SellerStoreEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerStoreRepository extends JpaRepository<SellerStoreEntity, UUID> {}
