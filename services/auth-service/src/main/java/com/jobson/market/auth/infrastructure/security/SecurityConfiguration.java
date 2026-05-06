package com.jobson.market.auth.infrastructure.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfiguration {

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      GoogleOAuth2SuccessHandler googleSuccessHandler,
      ObjectProvider<ClientRegistrationRepository> clientRegistrations)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(
                        "/auth/register",
                        "/auth/login",
                        "/auth/refresh",
                        "/.well-known/jwks.json",
                        "/oauth2/**",
                        "/login/oauth2/**",
                        "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> {}));

    clientRegistrations.ifAvailable(
        ignored -> http.oauth2Login(oauth -> oauth.successHandler(googleSuccessHandler)));

    return http.build();
  }
}
