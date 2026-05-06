package com.jobson.market.auth.application.port;

import com.jobson.market.auth.domain.model.User;

public interface AccessTokenIssuer {

  String issueAccessToken(User user);
}
