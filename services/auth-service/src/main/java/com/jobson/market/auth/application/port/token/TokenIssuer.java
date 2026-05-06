package com.jobson.market.auth.application.port.token;

import com.jobson.market.auth.domain.model.AuthTokens;
import com.jobson.market.auth.domain.model.User;

public interface TokenIssuer {

  AuthTokens issue(User user);
}
