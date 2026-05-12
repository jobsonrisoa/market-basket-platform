package com.jobson.market.customer_service.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobson.market.customer_service.TestcontainersConfiguration;
import com.jobson.market.customer_service.application.CustomerProfileService;
import com.jobson.market.customer_service.persistence.CustomerProfileRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CustomerProfileControllerIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private CustomerProfileRepository profiles;
  @Autowired private CustomerProfileService service;

  @BeforeEach
  void cleanDatabase() {
    profiles.deleteAll();
  }

  @Test
  void shouldAllowCustomerToReadAndUpdateOwnProfile() throws Exception {
    UUID userId = UUID.randomUUID();
    service.ensureProfileForRegisteredUser(userId);

    mvc.perform(get("/customers/me/profile").with(customerJwt(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authUserId").value(userId.toString()))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.defaultLocale").value("en-US"));

    mvc.perform(
            patch("/customers/me/profile")
                .with(customerJwt(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"displayName":"  Jane Market  ","phone":"+15551234567","defaultLocale":"pt-BR"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authUserId").value(userId.toString()))
        .andExpect(jsonPath("$.displayName").value("Jane Market"))
        .andExpect(jsonPath("$.phone").value("+15551234567"))
        .andExpect(jsonPath("$.defaultLocale").value("pt-BR"));
  }

  @Test
  void shouldPreventCustomerFromReadingAnotherProfile() throws Exception {
    UUID ownerUserId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    service.ensureProfileForRegisteredUser(ownerUserId);

    mvc.perform(get("/customers/profiles/{authUserId}", ownerUserId).with(customerJwt(otherUserId)))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldAllowSupportAndAdminsToReadProfiles() throws Exception {
    UUID ownerUserId = UUID.randomUUID();
    service.ensureProfileForRegisteredUser(ownerUserId);

    mvc.perform(
            get("/customers/profiles/{authUserId}", ownerUserId)
                .with(supportJwt(UUID.randomUUID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authUserId").value(ownerUserId.toString()));

    mvc.perform(get("/customers/profiles").with(adminJwt(UUID.randomUUID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].authUserId").value(ownerUserId.toString()));
  }

  @Test
  void shouldReturnNotFoundForMissingProfile() throws Exception {
    mvc.perform(get("/customers/me/profile").with(customerJwt(UUID.randomUUID())))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldRequireJwtForProfileApis() throws Exception {
    mvc.perform(get("/customers/me/profile")).andExpect(status().isUnauthorized());
  }

  private static RequestPostProcessor customerJwt(UUID subject) {
    return jwt()
        .jwt(token -> token.subject(subject.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }

  private static RequestPostProcessor supportJwt(UUID subject) {
    return jwt()
        .jwt(token -> token.subject(subject.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT_AGENT"));
  }

  private static RequestPostProcessor adminJwt(UUID subject) {
    return jwt()
        .jwt(token -> token.subject(subject.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }
}
