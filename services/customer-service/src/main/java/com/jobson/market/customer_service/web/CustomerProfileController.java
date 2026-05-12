package com.jobson.market.customer_service.web;

import com.jobson.market.customer_service.application.CustomerProfileService;
import com.jobson.market.customer_service.domain.CustomerProfileEntity;
import com.jobson.market.customer_service.domain.CustomerProfileStatus;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
class CustomerProfileController {

  private final CustomerProfileService profiles;

  CustomerProfileController(CustomerProfileService profiles) {
    this.profiles = profiles;
  }

  @GetMapping("/me/profile")
  CustomerProfileResponse getOwnProfile(Authentication authentication) {
    return CustomerProfileResponse.from(profiles.getOwnProfile(actorUserId(authentication)));
  }

  @PatchMapping("/me/profile")
  CustomerProfileResponse updateOwnProfile(
      @Valid @RequestBody UpdateCustomerProfileRequest request, Authentication authentication) {
    return CustomerProfileResponse.from(
        profiles.updateOwnProfile(
            actorUserId(authentication),
            request.displayName(),
            request.phone(),
            request.defaultLocale()));
  }

  @GetMapping("/profiles/{authUserId}")
  CustomerProfileResponse getProfile(@PathVariable UUID authUserId, Authentication authentication) {
    return CustomerProfileResponse.from(
        profiles.getProfileForSupport(
            authUserId, actorUserId(authentication), isPlatformSupport(authentication)));
  }

  @GetMapping("/profiles")
  List<CustomerProfileResponse> listProfiles(Authentication authentication) {
    return profiles.listProfilesForSupport(isPlatformSupport(authentication)).stream()
        .map(CustomerProfileResponse::from)
        .toList();
  }

  private static UUID actorUserId(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }

  private static boolean isPlatformSupport(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .anyMatch(
            authority ->
                "ROLE_SUPPORT_AGENT".equals(authority.getAuthority())
                    || "ROLE_ADMIN".equals(authority.getAuthority())
                    || "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
  }

  record UpdateCustomerProfileRequest(String displayName, String phone, String defaultLocale) {}

  record CustomerProfileResponse(
      UUID id,
      UUID authUserId,
      String displayName,
      String phone,
      String defaultLocale,
      CustomerProfileStatus status,
      Instant createdAt,
      Instant updatedAt) {
    static CustomerProfileResponse from(CustomerProfileEntity profile) {
      return new CustomerProfileResponse(
          profile.id(),
          profile.authUserId(),
          profile.displayName(),
          profile.phone(),
          profile.defaultLocale(),
          profile.status(),
          profile.createdAt(),
          profile.updatedAt());
    }
  }
}
