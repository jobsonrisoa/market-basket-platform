package com.jobson.market.seller_service.web;

import com.jobson.market.seller_service.application.SellerService;
import com.jobson.market.seller_service.domain.SellerApprovalStatus;
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
import org.springframework.security.core.Authentication;
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
  ResponseEntity<SellerResponse> createSeller(
      @Valid @RequestBody CreateSellerRequest request, Authentication authentication) {
    SellerStoreEntity store = sellers.createStore(request.name(), actorUserId(authentication));
    return ResponseEntity.status(HttpStatus.CREATED).body(SellerResponse.from(store));
  }

  @GetMapping("/{sellerId}")
  SellerResponse getSeller(@PathVariable UUID sellerId, Authentication authentication) {
    return SellerResponse.from(
        sellers.getStore(sellerId, actorUserId(authentication), isPlatformAdmin(authentication)));
  }

  @PostMapping("/{sellerId}/approve")
  SellerResponse approveSeller(
      @PathVariable UUID sellerId,
      @Valid @RequestBody ReviewSellerRequest request,
      Authentication authentication) {
    return SellerResponse.from(
        sellers.approveStore(sellerId, actorUserId(authentication), request.reviewNotes()));
  }

  @PostMapping("/{sellerId}/reject")
  SellerResponse rejectSeller(
      @PathVariable UUID sellerId,
      @Valid @RequestBody ReviewSellerRequest request,
      Authentication authentication) {
    return SellerResponse.from(
        sellers.rejectStore(sellerId, actorUserId(authentication), request.reviewNotes()));
  }

  @PostMapping("/{sellerId}/members")
  ResponseEntity<SellerMembershipResponse> addMember(
      @PathVariable UUID sellerId,
      @Valid @RequestBody AddMemberRequest request,
      Authentication authentication) {
    SellerMembershipEntity membership =
        sellers.addMember(
            sellerId,
            actorUserId(authentication),
            isPlatformAdmin(authentication),
            request.userId(),
            request.role());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SellerMembershipResponse.from(membership));
  }

  @GetMapping("/{sellerId}/members")
  List<SellerMembershipResponse> listMembers(
      @PathVariable UUID sellerId, Authentication authentication) {
    return sellers
        .listMembers(sellerId, actorUserId(authentication), isPlatformAdmin(authentication))
        .stream()
        .map(SellerMembershipResponse::from)
        .toList();
  }

  @DeleteMapping("/{sellerId}/members/{userId}")
  ResponseEntity<Void> removeMember(
      @PathVariable UUID sellerId, @PathVariable UUID userId, Authentication authentication) {
    sellers.removeMember(
        sellerId, actorUserId(authentication), isPlatformAdmin(authentication), userId);
    return ResponseEntity.noContent().build();
  }

  private static UUID actorUserId(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }

  private static boolean isPlatformAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .anyMatch(
            authority ->
                "ROLE_ADMIN".equals(authority.getAuthority())
                    || "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
  }

  record CreateSellerRequest(@NotBlank String name, UUID ownerUserId) {}

  record AddMemberRequest(@NotNull UUID userId, @NotNull SellerMembershipRole role) {}

  record ReviewSellerRequest(UUID reviewerUserId, String reviewNotes) {}

  record SellerResponse(
      UUID id,
      String name,
      UUID ownerUserId,
      SellerApprovalStatus approvalStatus,
      Instant createdAt,
      Instant submittedAt,
      Instant reviewedAt,
      UUID reviewedByUserId,
      String reviewNotes) {
    static SellerResponse from(SellerStoreEntity store) {
      return new SellerResponse(
          store.id(),
          store.name(),
          store.ownerUserId(),
          store.approvalStatus(),
          store.createdAt(),
          store.submittedAt(),
          store.reviewedAt(),
          store.reviewedByUserId(),
          store.reviewNotes());
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
