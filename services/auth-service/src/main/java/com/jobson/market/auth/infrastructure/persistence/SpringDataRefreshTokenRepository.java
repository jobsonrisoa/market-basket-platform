package com.jobson.market.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  List<RefreshTokenEntity> findByFamilyId(UUID familyId);
}
