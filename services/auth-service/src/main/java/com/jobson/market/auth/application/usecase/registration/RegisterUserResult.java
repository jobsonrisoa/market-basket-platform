package com.jobson.market.auth.application.usecase.registration;

import com.jobson.market.auth.domain.model.AccountProfile;
import com.jobson.market.auth.domain.model.CustomerProfileType;
import com.jobson.market.auth.domain.model.Permission;
import com.jobson.market.auth.domain.model.Role;
import com.jobson.market.auth.domain.model.User;
import java.util.Set;
import java.util.UUID;

public record RegisterUserResult(
    UUID userId,
    String email,
    Set<Role> roles,
    Set<Permission> permissions,
    AccountProfile accountProfile,
    CustomerProfileType customerProfileType) {

  public static RegisterUserResult from(User user) {
    return new RegisterUserResult(
        user.id(),
        user.email().value(),
        user.roles(),
        user.permissions(),
        user.accountProfile(),
        user.customerProfileType());
  }
}
