package com.jobson.market.inventory_service.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

final class AuthenticatedSellerAccess {

  private AuthenticatedSellerAccess() {}

  static void requireSellerAccess(Authentication authentication, UUID sellerId) {
    if (hasAnyRole(authentication, "ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_SERVICE")) {
      return;
    }
    Jwt jwt = (Jwt) authentication.getPrincipal();
    List<Map<String, Object>> memberships = jwt.getClaim("seller_memberships");
    if (memberships == null) {
      throw new AccessDeniedException("Active seller membership required");
    }
    boolean activeMember =
        memberships.stream()
            .anyMatch(
                membership ->
                    sellerId.toString().equals(String.valueOf(membership.get("sellerId")))
                        && "ACTIVE".equals(String.valueOf(membership.get("status"))));
    if (!activeMember) {
      throw new AccessDeniedException("Active seller membership required");
    }
  }

  private static boolean hasAnyRole(Authentication authentication, String... roles) {
    for (String role : roles) {
      boolean found =
          authentication.getAuthorities().stream()
              .anyMatch(authority -> role.equals(authority.getAuthority()));
      if (found) {
        return true;
      }
    }
    return false;
  }
}
