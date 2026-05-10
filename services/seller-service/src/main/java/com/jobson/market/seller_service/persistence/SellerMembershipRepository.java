package com.jobson.market.seller_service.persistence;

import com.jobson.market.seller_service.domain.SellerMembershipEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerMembershipRepository extends JpaRepository<SellerMembershipEntity, UUID> {

  List<SellerMembershipEntity> findBySellerIdOrderByCreatedAtAsc(UUID sellerId);

  Optional<SellerMembershipEntity> findBySellerIdAndUserId(UUID sellerId, UUID userId);
}
