package com.jobson.market.seller_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobson.market.seller_service.domain.SellerStoreEntity;
import com.jobson.market.seller_service.event.SellerEvent;
import com.jobson.market.seller_service.event.SellerEventPublisher;
import com.jobson.market.seller_service.persistence.SellerMembershipRepository;
import com.jobson.market.seller_service.persistence.SellerStoreRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SellerServiceTest {

  @Test
  void shouldPublishSellerApprovedEventWhenApprovingStore() {
    SellerStoreRepository stores = mock(SellerStoreRepository.class);
    SellerMembershipRepository memberships = mock(SellerMembershipRepository.class);
    RecordingSellerEventPublisher publisher = new RecordingSellerEventPublisher();
    SellerService service =
        new SellerService(
            stores,
            memberships,
            Clock.fixed(Instant.parse("2026-05-10T13:00:00Z"), ZoneOffset.UTC),
            publisher);
    UUID reviewerUserId = UUID.randomUUID();
    SellerStoreEntity store =
        SellerStoreEntity.create(
            "Fresh Market", UUID.randomUUID(), Instant.parse("2026-05-10T12:00:00Z"));
    when(stores.findById(store.id())).thenReturn(Optional.of(store));
    when(stores.save(any(SellerStoreEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.approveStore(store.id(), reviewerUserId, "Ready");

    verify(stores).save(store);
    assertEquals("seller.approved.v1", publisher.event.eventType());
    assertEquals(store.id().toString(), publisher.event.payload().sellerId());
    assertEquals(reviewerUserId.toString(), publisher.event.payload().reviewedByUserId());
  }

  @Test
  void shouldPublishSellerRejectedEventWhenRejectingStore() {
    SellerStoreRepository stores = mock(SellerStoreRepository.class);
    SellerMembershipRepository memberships = mock(SellerMembershipRepository.class);
    RecordingSellerEventPublisher publisher = new RecordingSellerEventPublisher();
    SellerService service =
        new SellerService(
            stores,
            memberships,
            Clock.fixed(Instant.parse("2026-05-10T13:00:00Z"), ZoneOffset.UTC),
            publisher);
    UUID reviewerUserId = UUID.randomUUID();
    SellerStoreEntity store =
        SellerStoreEntity.create(
            "Fresh Market", UUID.randomUUID(), Instant.parse("2026-05-10T12:00:00Z"));
    when(stores.findById(store.id())).thenReturn(Optional.of(store));
    when(stores.save(any(SellerStoreEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.rejectStore(store.id(), reviewerUserId, "Needs documents");

    verify(stores).save(store);
    assertEquals("seller.rejected.v1", publisher.event.eventType());
    assertEquals("REJECTED", publisher.event.payload().approvalStatus());
    assertEquals(store.id().toString(), publisher.event.payload().sellerId());
    assertEquals(reviewerUserId.toString(), publisher.event.payload().reviewedByUserId());
  }

  private static class RecordingSellerEventPublisher implements SellerEventPublisher {
    private SellerEvent event;

    @Override
    public void publish(SellerEvent event) {
      this.event = event;
    }
  }
}
