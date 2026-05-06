package com.jobson.market.auth.application.port.crypto;

import com.jobson.market.auth.domain.model.Password;

public interface PasswordHasher {

  String hash(Password password);
}
