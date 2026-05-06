package com.jobson.market.auth.infrastructure.jwt;

import com.jobson.market.auth.infrastructure.config.AuthProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JwtKeyStore {

  private final RSAKey rsaKey;

  JwtKeyStore(AuthProperties properties) {
    KeyPair keyPair = generateRsaKeyPair();
    this.rsaKey =
        new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey((RSAPrivateKey) keyPair.getPrivate())
            .keyID(properties.jwt().keyId())
            .build();
  }

  public RSAKey privateKey() {
    return rsaKey;
  }

  public RSAPublicKey publicKey() {
    try {
      return rsaKey.toRSAPublicKey();
    } catch (JOSEException exception) {
      throw new IllegalStateException("Unable to export RSA public key", exception);
    }
  }

  public Map<String, Object> publicJwks() {
    return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
  }

  private static KeyPair generateRsaKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("RSA is not available", exception);
    }
  }
}
