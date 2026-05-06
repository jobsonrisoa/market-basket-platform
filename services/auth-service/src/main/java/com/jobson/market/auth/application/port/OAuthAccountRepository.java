package com.jobson.market.auth.application.port;

import com.jobson.market.auth.domain.model.OAuthAccount;
import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountRepository {

  Optional<OAuthAccount> findByProviderAndSubject(String provider, String providerSubject);

  OAuthAccount save(OAuthAccount account);

  boolean existsByUserIdAndProvider(UUID userId, String provider);
}
