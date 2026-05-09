package com.jobson.market.auth.domain.model;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record User(
    UUID id,
    Email email,
    boolean emailVerified,
    UserStatus status,
    Set<Role> roles,
    AccountProfile accountProfile,
    CustomerProfileType customerProfileType) {

  public User {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(email, "email is required");
    Objects.requireNonNull(status, "status is required");
    Objects.requireNonNull(roles, "roles are required");
    Objects.requireNonNull(accountProfile, "accountProfile is required");
    roles = roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
    if (accountProfile == AccountProfile.CUSTOMER) {
      Objects.requireNonNull(customerProfileType, "customerProfileType is required for customers");
    }
  }

  public User(UUID id, Email email, boolean emailVerified, UserStatus status) {
    this(
        id,
        email,
        emailVerified,
        status,
        EnumSet.of(Role.CUSTOMER),
        AccountProfile.CUSTOMER,
        CustomerProfileType.INDIVIDUAL);
  }

  public static User register(Email email) {
    return new User(
        UUID.randomUUID(),
        email,
        false,
        UserStatus.PENDING_EMAIL_VERIFICATION,
        EnumSet.of(Role.CUSTOMER),
        AccountProfile.CUSTOMER,
        CustomerProfileType.INDIVIDUAL);
  }

  public static User admin(Email email) {
    return new User(
        UUID.randomUUID(),
        email,
        true,
        UserStatus.ACTIVE,
        EnumSet.of(Role.ADMIN),
        AccountProfile.PLATFORM,
        null);
  }

  public static User superAdmin(Email email) {
    return new User(
        UUID.randomUUID(),
        email,
        true,
        UserStatus.ACTIVE,
        EnumSet.of(Role.SUPER_ADMIN),
        AccountProfile.PLATFORM,
        null);
  }

  public User verifyEmail() {
    return new User(id, email, true, UserStatus.ACTIVE, roles, accountProfile, customerProfileType);
  }

  public User assignRole(Role role) {
    EnumSet<Role> updatedRoles =
        roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
    updatedRoles.add(Objects.requireNonNull(role, "role is required"));
    AccountProfile updatedProfile = profileFor(updatedRoles);
    CustomerProfileType updatedCustomerProfileType =
        updatedProfile == AccountProfile.CUSTOMER ? customerProfileType : null;
    return new User(
        id, email, emailVerified, status, updatedRoles, updatedProfile, updatedCustomerProfileType);
  }

  public User removeRole(Role role) {
    EnumSet<Role> updatedRoles =
        roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
    updatedRoles.remove(Objects.requireNonNull(role, "role is required"));
    if (updatedRoles.isEmpty()) {
      updatedRoles.add(Role.CUSTOMER);
    }
    AccountProfile updatedProfile = profileFor(updatedRoles);
    CustomerProfileType updatedCustomerProfileType =
        updatedProfile == AccountProfile.CUSTOMER
            ? Objects.requireNonNullElse(customerProfileType, CustomerProfileType.INDIVIDUAL)
            : null;
    return new User(
        id, email, emailVerified, status, updatedRoles, updatedProfile, updatedCustomerProfileType);
  }

  public User suspend() {
    return new User(
        id, email, emailVerified, UserStatus.SUSPENDED, roles, accountProfile, customerProfileType);
  }

  public User reactivate() {
    return new User(
        id, email, emailVerified, UserStatus.ACTIVE, roles, accountProfile, customerProfileType);
  }

  public boolean hasRole(Role role) {
    return roles.contains(role);
  }

  public Set<Permission> permissions() {
    EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
    roles.forEach(role -> permissions.addAll(role.permissions()));
    return permissions.isEmpty() ? Set.of() : EnumSet.copyOf(permissions);
  }

  public boolean hasPermission(Permission permission) {
    return permissions().contains(permission);
  }

  public boolean canLogin() {
    return status != UserStatus.LOCKED
        && status != UserStatus.SUSPENDED
        && status != UserStatus.DISABLED;
  }

  private static AccountProfile profileFor(Set<Role> roles) {
    if (roles.stream().anyMatch(Role::isAdministrative)) {
      return AccountProfile.PLATFORM;
    }
    if (roles.stream().anyMatch(Role::isSeller)) {
      return AccountProfile.SELLER;
    }
    return AccountProfile.CUSTOMER;
  }
}
