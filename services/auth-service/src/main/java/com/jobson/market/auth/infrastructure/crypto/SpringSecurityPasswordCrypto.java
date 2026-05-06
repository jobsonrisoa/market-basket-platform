package com.jobson.market.auth.infrastructure.crypto;

import com.jobson.market.auth.application.port.crypto.PasswordHasher;
import com.jobson.market.auth.application.port.crypto.PasswordVerifier;
import com.jobson.market.auth.domain.model.Password;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class SpringSecurityPasswordCrypto implements PasswordHasher, PasswordVerifier {

  private final PasswordEncoder passwordEncoder;

  SpringSecurityPasswordCrypto(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public String hash(Password password) {
    return passwordEncoder.encode(password.value());
  }

  @Override
  public boolean matches(Password rawPassword, String passwordHash) {
    return passwordEncoder.matches(rawPassword.value(), passwordHash);
  }
}
