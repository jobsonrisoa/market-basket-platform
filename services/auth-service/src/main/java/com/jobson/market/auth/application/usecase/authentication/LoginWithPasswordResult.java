package com.jobson.market.auth.application.usecase.authentication;

import com.jobson.market.auth.domain.model.AccountProfile;
import com.jobson.market.auth.domain.model.AuthTokens;
import com.jobson.market.auth.domain.model.CustomerProfileType;
import com.jobson.market.auth.domain.model.Permission;
import com.jobson.market.auth.domain.model.Role;
import com.jobson.market.auth.domain.model.User;
import java.util.Set;

public record LoginWithPasswordResult(
    String accessToken,
    String refreshToken,
    Set<Role> roles,
    Set<Permission> permissions,
    AccountProfile accountProfile,
    CustomerProfileType customerProfileType) {

  public static LoginWithPasswordResult from(AuthTokens tokens, User user) {
    return new LoginWithPasswordResult(
        tokens.accessToken(),
        tokens.refreshToken(),
        user.roles(),
        user.permissions(),
        user.accountProfile(),
        user.customerProfileType());
  }
}
