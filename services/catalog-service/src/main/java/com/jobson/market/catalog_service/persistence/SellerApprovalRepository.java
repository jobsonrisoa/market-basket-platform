package com.jobson.market.catalog_service.persistence;

import com.jobson.market.catalog_service.domain.SellerApprovalState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerApprovalRepository extends JpaRepository<SellerApprovalState, UUID> {

  Optional<SellerApprovalState> findBySellerId(UUID sellerId);
}
