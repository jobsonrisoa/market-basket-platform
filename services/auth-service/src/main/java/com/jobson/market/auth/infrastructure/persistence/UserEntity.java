package com.jobson.market.auth.infrastructure.persistence;

import com.jobson.market.auth.domain.model.AccountProfile;
import com.jobson.market.auth.domain.model.CustomerProfileType;
import com.jobson.market.auth.domain.model.Role;
import com.jobson.market.auth.domain.model.User;
import com.jobson.market.auth.domain.model.UserStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private boolean emailVerified;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Set<Role> roles = EnumSet.noneOf(Role.class);

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountProfile accountProfile;

  @Enumerated(EnumType.STRING)
  private CustomerProfileType customerProfileType;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected UserEntity() {}

  static UserEntity fromDomain(User user, Instant createdAt, Instant updatedAt) {
    UserEntity entity = new UserEntity();
    entity.id = user.id();
    entity.email = user.email().value();
    entity.emailVerified = user.emailVerified();
    entity.status = user.status();
    entity.roles = copyRoles(user.roles());
    entity.accountProfile = user.accountProfile();
    entity.customerProfileType = user.customerProfileType();
    entity.createdAt = createdAt;
    entity.updatedAt = updatedAt;
    return entity;
  }

  UUID id() {
    return id;
  }

  String email() {
    return email;
  }

  boolean emailVerified() {
    return emailVerified;
  }

  UserStatus status() {
    return status;
  }

  Set<Role> roles() {
    return roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
  }

  AccountProfile accountProfile() {
    return accountProfile;
  }

  CustomerProfileType customerProfileType() {
    return customerProfileType;
  }

  Instant createdAt() {
    return createdAt;
  }

  private static Set<Role> copyRoles(Set<Role> roles) {
    return roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
  }
}
