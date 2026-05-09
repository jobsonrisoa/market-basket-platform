package com.jobson.market.auth.infrastructure.persistence.identity;

import com.jobson.market.auth.application.port.identity.OAuthAccountRepository;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.OAuthAccount;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaOAuthAccountRepository implements OAuthAccountRepository {

  private final SpringDataOAuthAccountRepository accounts;
  private final Clock clock;

  JpaOAuthAccountRepository(SpringDataOAuthAccountRepository accounts, Clock clock) {
    this.accounts = accounts;
    this.clock = clock;
  }

  @Override
  public Optional<OAuthAccount> findByProviderAndSubject(String provider, String providerSubject) {
    return accounts.findByProviderAndProviderSubject(provider, providerSubject).map(this::toDomain);
  }

  @Override
  public OAuthAccount save(OAuthAccount account) {
    return toDomain(
        accounts.save(
            new OAuthAccountEntity(
                account.id(),
                account.userId(),
                account.provider(),
                account.providerSubject(),
                account.email().value(),
                clock.instant())));
  }

  @Override
  public boolean existsByUserIdAndProvider(UUID userId, String provider) {
    return accounts.existsByUserIdAndProvider(userId, provider);
  }

  private OAuthAccount toDomain(OAuthAccountEntity entity) {
    return new OAuthAccount(
        entity.id(),
        entity.userId(),
        entity.provider(),
        entity.providerSubject(),
        new Email(entity.email()));
  }
}
