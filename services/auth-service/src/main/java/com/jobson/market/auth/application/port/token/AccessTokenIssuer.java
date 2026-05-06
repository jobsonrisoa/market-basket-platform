package com.jobson.market.auth.application.port.token;

import com.jobson.market.auth.domain.model.User;

public interface AccessTokenIssuer {

  String issueAccessToken(User user);
}
