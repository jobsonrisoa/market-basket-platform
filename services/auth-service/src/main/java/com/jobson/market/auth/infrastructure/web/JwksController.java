package com.jobson.market.auth.infrastructure.web;

import com.jobson.market.auth.infrastructure.jwt.JwtKeyStore;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class JwksController {

  private final JwtKeyStore keyStore;

  JwksController(JwtKeyStore keyStore) {
    this.keyStore = keyStore;
  }

  @GetMapping("/.well-known/jwks.json")
  Map<String, Object> jwks() {
    return keyStore.publicJwks();
  }
}
