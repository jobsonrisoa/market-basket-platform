package com.jobson.market.auth.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
  CUSTOMER(Permission.CUSTOMER_PROFILE_ACCESS, Permission.CUSTOMER_SUBSCRIPTION_MANAGE_OWN),
  SELLER_OWNER(
      Permission.SELLER_CATALOG_MANAGE,
      Permission.SELLER_INVENTORY_MANAGE,
      Permission.SELLER_ORDER_FULFILL,
      Permission.SELLER_STAFF_MANAGE),
  SELLER_STAFF(
      Permission.SELLER_CATALOG_MANAGE,
      Permission.SELLER_INVENTORY_MANAGE,
      Permission.SELLER_ORDER_FULFILL),
  SUPPORT_AGENT(Permission.SUPPORT_CUSTOMER_READ, Permission.SUPPORT_ORDER_ASSIST),
  ADMIN(
      Permission.PLATFORM_SELLER_REVIEW,
      Permission.AUTH_USER_ROLE_ASSIGN,
      Permission.AUTH_USER_ROLE_REVOKE),
  SUPER_ADMIN(
      Permission.PLATFORM_SELLER_REVIEW,
      Permission.AUTH_USER_ROLE_ASSIGN,
      Permission.AUTH_USER_ROLE_REVOKE);

  private final Set<Permission> permissions;

  Role(Permission... permissions) {
    this.permissions = permissions.length == 0 ? Set.of() : EnumSet.copyOf(Set.of(permissions));
  }

  public Set<Permission> permissions() {
    return permissions.isEmpty() ? Set.of() : EnumSet.copyOf(permissions);
  }

  public boolean isAdministrative() {
    return this == ADMIN || this == SUPER_ADMIN || this == SUPPORT_AGENT;
  }

  public boolean isSeller() {
    return this == SELLER_OWNER || this == SELLER_STAFF;
  }

  public boolean isSecuritySensitive() {
    return this == ADMIN || this == SUPER_ADMIN;
  }
}
