package com.jobson.market.auth.infrastructure.persistence.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOAuthAccountRepository extends JpaRepository<OAuthAccountEntity, UUID> {

  Optional<OAuthAccountEntity> findByProviderAndProviderSubject(
      String provider, String providerSubject);

  boolean existsByUserIdAndProvider(UUID userId, String provider);
}
