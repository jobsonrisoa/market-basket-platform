package com.jobson.market.auth.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRefreshTokenFamilyRepository
    extends JpaRepository<RefreshTokenFamilyEntity, UUID> {

  List<RefreshTokenFamilyEntity> findByUserIdAndStatus(UUID userId, String status);
}
