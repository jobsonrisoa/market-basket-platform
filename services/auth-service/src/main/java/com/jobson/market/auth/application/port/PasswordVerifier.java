package com.jobson.market.auth.application.port;

import com.jobson.market.auth.domain.model.Password;

public interface PasswordVerifier {

  boolean matches(Password rawPassword, String passwordHash);
}
