package com.jobson.market.auth.infrastructure.jwt;

import com.jobson.market.auth.infrastructure.config.AuthProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
class JwtConfiguration {

  @Bean
  JwtEncoder jwtEncoder(JwtKeyStore keyStore) {
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(keyStore.privateKey())));
  }

  @Bean
  JwtDecoder jwtDecoder(JwtKeyStore keyStore, AuthProperties properties) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keyStore.publicKey()).build();
    OAuth2TokenValidator<Jwt> issuer =
        JwtValidators.createDefaultWithIssuer(properties.jwt().issuer());
    OAuth2TokenValidator<Jwt> audience =
        new JwtClaimValidator<List<String>>(
            "aud",
            audiences -> audiences != null && audiences.contains(properties.jwt().audience()));
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
    return decoder;
  }
}
