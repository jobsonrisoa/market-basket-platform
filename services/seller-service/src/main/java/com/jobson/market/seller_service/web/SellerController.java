package com.jobson.market.seller_service.web;

import com.jobson.market.seller_service.application.SellerService;
import com.jobson.market.seller_service.domain.SellerMembershipEntity;
import com.jobson.market.seller_service.domain.SellerMembershipRole;
import com.jobson.market.seller_service.domain.SellerMembershipStatus;
import com.jobson.market.seller_service.domain.SellerStoreEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sellers")
class SellerController {

  private final SellerService sellers;

  SellerController(SellerService sellers) {
    this.sellers = sellers;
  }

  @PostMapping
  ResponseEntity<SellerResponse> createSeller(@Valid @RequestBody CreateSellerRequest request) {
    SellerStoreEntity store = sellers.createStore(request.name(), request.ownerUserId());
    return ResponseEntity.status(HttpStatus.CREATED).body(SellerResponse.from(store));
  }

  @GetMapping("/{sellerId}")
  SellerResponse getSeller(@PathVariable UUID sellerId) {
    return SellerResponse.from(sellers.getStore(sellerId));
  }

  @PostMapping("/{sellerId}/members")
  ResponseEntity<SellerMembershipResponse> addMember(
      @PathVariable UUID sellerId, @Valid @RequestBody AddMemberRequest request) {
    SellerMembershipEntity membership =
        sellers.addMember(sellerId, request.userId(), request.role());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SellerMembershipResponse.from(membership));
  }

  @GetMapping("/{sellerId}/members")
  List<SellerMembershipResponse> listMembers(@PathVariable UUID sellerId) {
    return sellers.listMembers(sellerId).stream().map(SellerMembershipResponse::from).toList();
  }

  @DeleteMapping("/{sellerId}/members/{userId}")
  ResponseEntity<Void> removeMember(@PathVariable UUID sellerId, @PathVariable UUID userId) {
    sellers.removeMember(sellerId, userId);
    return ResponseEntity.noContent().build();
  }

  record CreateSellerRequest(@NotBlank String name, @NotNull UUID ownerUserId) {}

  record AddMemberRequest(@NotNull UUID userId, @NotNull SellerMembershipRole role) {}

  record SellerResponse(UUID id, String name, UUID ownerUserId, Instant createdAt) {
    static SellerResponse from(SellerStoreEntity store) {
      return new SellerResponse(store.id(), store.name(), store.ownerUserId(), store.createdAt());
    }
  }

  record SellerMembershipResponse(
      UUID id,
      UUID sellerId,
      UUID userId,
      SellerMembershipRole role,
      SellerMembershipStatus status,
      Instant createdAt,
      Instant removedAt) {
    static SellerMembershipResponse from(SellerMembershipEntity membership) {
      return new SellerMembershipResponse(
          membership.id(),
          membership.sellerId(),
          membership.userId(),
          membership.role(),
          membership.status(),
          membership.createdAt(),
          membership.removedAt());
    }
  }
}
