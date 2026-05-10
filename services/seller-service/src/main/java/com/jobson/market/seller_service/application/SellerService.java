package com.jobson.market.seller_service.application;

import com.jobson.market.seller_service.domain.SellerMembershipEntity;
import com.jobson.market.seller_service.domain.SellerMembershipRole;
import com.jobson.market.seller_service.domain.SellerStoreEntity;
import com.jobson.market.seller_service.persistence.SellerMembershipRepository;
import com.jobson.market.seller_service.persistence.SellerStoreRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerService {

  private final SellerStoreRepository stores;
  private final SellerMembershipRepository memberships;
  private final Clock clock;

  public SellerService(
      SellerStoreRepository stores, SellerMembershipRepository memberships, Clock clock) {
    this.stores = stores;
    this.memberships = memberships;
    this.clock = clock;
  }

  @Transactional
  public SellerStoreEntity createStore(String name, UUID ownerUserId) {
    SellerStoreEntity store =
        stores.save(SellerStoreEntity.create(name, ownerUserId, clock.instant()));
    memberships.save(
        SellerMembershipEntity.active(
            store.id(), ownerUserId, SellerMembershipRole.OWNER, clock.instant()));
    return store;
  }

  @Transactional(readOnly = true)
  public SellerStoreEntity getStore(UUID sellerId) {
    return stores.findById(sellerId).orElseThrow(() -> new SellerNotFoundException(sellerId));
  }

  @Transactional
  public SellerMembershipEntity addMember(UUID sellerId, UUID userId, SellerMembershipRole role) {
    getStore(sellerId);
    return memberships
        .findBySellerIdAndUserId(sellerId, userId)
        .map(
            membership -> {
              membership.activateAs(role);
              return memberships.save(membership);
            })
        .orElseGet(
            () ->
                memberships.save(
                    SellerMembershipEntity.active(sellerId, userId, role, clock.instant())));
  }

  @Transactional(readOnly = true)
  public List<SellerMembershipEntity> listMembers(UUID sellerId) {
    getStore(sellerId);
    return memberships.findBySellerIdOrderByCreatedAtAsc(sellerId);
  }

  @Transactional
  public void removeMember(UUID sellerId, UUID userId) {
    getStore(sellerId);
    memberships
        .findBySellerIdAndUserId(sellerId, userId)
        .ifPresent(
            membership -> {
              membership.remove(clock.instant());
              memberships.save(membership);
            });
  }
}
