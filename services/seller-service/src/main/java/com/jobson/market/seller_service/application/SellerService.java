package com.jobson.market.seller_service.application;

import com.jobson.market.seller_service.domain.SellerMembershipEntity;
import com.jobson.market.seller_service.domain.SellerMembershipRole;
import com.jobson.market.seller_service.domain.SellerMembershipStatus;
import com.jobson.market.seller_service.domain.SellerStoreEntity;
import com.jobson.market.seller_service.event.SellerEvent;
import com.jobson.market.seller_service.event.SellerEventPublisher;
import com.jobson.market.seller_service.persistence.SellerMembershipRepository;
import com.jobson.market.seller_service.persistence.SellerStoreRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerService {

  private final SellerStoreRepository stores;
  private final SellerMembershipRepository memberships;
  private final Clock clock;
  private final SellerEventPublisher events;

  public SellerService(
      SellerStoreRepository stores,
      SellerMembershipRepository memberships,
      Clock clock,
      SellerEventPublisher events) {
    this.stores = stores;
    this.memberships = memberships;
    this.clock = clock;
    this.events = events;
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
  public SellerStoreEntity getStore(UUID sellerId, UUID actorUserId, boolean platformAdmin) {
    SellerStoreEntity store = requireStore(sellerId);
    requireActiveMemberOrAdmin(sellerId, actorUserId, platformAdmin);
    return store;
  }

  @Transactional
  public SellerMembershipEntity addMember(
      UUID sellerId,
      UUID actorUserId,
      boolean platformAdmin,
      UUID userId,
      SellerMembershipRole role) {
    requireStore(sellerId);
    requireOwnerOrAdmin(sellerId, actorUserId, platformAdmin);
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
  public List<SellerMembershipEntity> listMembers(
      UUID sellerId, UUID actorUserId, boolean platformAdmin) {
    requireStore(sellerId);
    requireOwnerOrAdmin(sellerId, actorUserId, platformAdmin);
    return memberships.findBySellerIdOrderByCreatedAtAsc(sellerId);
  }

  @Transactional
  public void removeMember(UUID sellerId, UUID actorUserId, boolean platformAdmin, UUID userId) {
    requireStore(sellerId);
    requireOwnerOrAdmin(sellerId, actorUserId, platformAdmin);
    memberships
        .findBySellerIdAndUserId(sellerId, userId)
        .ifPresent(
            membership -> {
              membership.remove(clock.instant());
              memberships.save(membership);
            });
  }

  @Transactional
  public SellerStoreEntity approveStore(UUID sellerId, UUID reviewerUserId, String reviewNotes) {
    SellerStoreEntity store = requireStore(sellerId);
    store.approve(reviewerUserId, reviewNotes, clock.instant());
    SellerStoreEntity saved = stores.save(store);
    events.publish(SellerEvent.sellerApproved(saved));
    return saved;
  }

  @Transactional
  public SellerStoreEntity rejectStore(UUID sellerId, UUID reviewerUserId, String reviewNotes) {
    SellerStoreEntity store = requireStore(sellerId);
    store.reject(reviewerUserId, reviewNotes, clock.instant());
    SellerStoreEntity saved = stores.save(store);
    events.publish(SellerEvent.sellerRejected(saved));
    return saved;
  }

  private SellerStoreEntity requireStore(UUID sellerId) {
    return stores.findById(sellerId).orElseThrow(() -> new SellerNotFoundException(sellerId));
  }

  private void requireActiveMemberOrAdmin(UUID sellerId, UUID actorUserId, boolean platformAdmin) {
    if (platformAdmin) {
      return;
    }
    memberships
        .findBySellerIdAndUserId(sellerId, actorUserId)
        .filter(membership -> membership.status() == SellerMembershipStatus.ACTIVE)
        .orElseThrow(() -> new AccessDeniedException("Active seller membership required"));
  }

  private void requireOwnerOrAdmin(UUID sellerId, UUID actorUserId, boolean platformAdmin) {
    if (platformAdmin) {
      return;
    }
    memberships
        .findBySellerIdAndUserId(sellerId, actorUserId)
        .filter(membership -> membership.status() == SellerMembershipStatus.ACTIVE)
        .filter(membership -> membership.role() == SellerMembershipRole.OWNER)
        .orElseThrow(() -> new AccessDeniedException("Active seller owner membership required"));
  }
}
