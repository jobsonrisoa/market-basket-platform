package com.jobson.market.customer_service.application;

import com.jobson.market.customer_service.domain.CustomerProfileEntity;
import com.jobson.market.customer_service.persistence.CustomerProfileRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerProfileService {

  private final CustomerProfileRepository profiles;
  private final Clock clock;

  public CustomerProfileService(CustomerProfileRepository profiles, Clock clock) {
    this.profiles = profiles;
    this.clock = clock;
  }

  @Transactional
  public CustomerProfileEntity ensureProfileForRegisteredUser(UUID authUserId) {
    return profiles
        .findByAuthUserId(authUserId)
        .orElseGet(
            () -> profiles.save(CustomerProfileEntity.initialFor(authUserId, clock.instant())));
  }

  @Transactional(readOnly = true)
  public CustomerProfileEntity getOwnProfile(UUID actorUserId) {
    return requireProfile(actorUserId);
  }

  @Transactional
  public CustomerProfileEntity updateOwnProfile(
      UUID actorUserId, String displayName, String phone, String defaultLocale) {
    CustomerProfileEntity profile = requireProfile(actorUserId);
    profile.update(displayName, phone, defaultLocale, clock.instant());
    return profiles.save(profile);
  }

  @Transactional(readOnly = true)
  public CustomerProfileEntity getProfileForSupport(
      UUID authUserId, UUID actorUserId, boolean platformSupport) {
    if (!platformSupport && !authUserId.equals(actorUserId)) {
      throw new AccessDeniedException("Customer profile ownership required");
    }
    return requireProfile(authUserId);
  }

  @Transactional(readOnly = true)
  public List<CustomerProfileEntity> listProfilesForSupport(boolean platformSupport) {
    if (!platformSupport) {
      throw new AccessDeniedException("Platform support role required");
    }
    return profiles.findAll();
  }

  private CustomerProfileEntity requireProfile(UUID authUserId) {
    return profiles
        .findByAuthUserId(authUserId)
        .orElseThrow(() -> new CustomerProfileNotFoundException(authUserId));
  }
}
