package com.jobson.market.customer_service.persistence;

import com.jobson.market.customer_service.domain.CustomerProfileEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, UUID> {

  Optional<CustomerProfileEntity> findByAuthUserId(UUID authUserId);

  boolean existsByAuthUserId(UUID authUserId);
}
