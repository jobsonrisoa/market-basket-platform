package unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.auth.domain.model.AccountProfile;
import com.jobson.market.auth.domain.model.CustomerProfileType;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.Role;
import com.jobson.market.auth.domain.model.User;
import com.jobson.market.auth.domain.model.UserStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void shouldCreatePendingUserWithNormalizedEmail() {
    User user = User.register(new Email("  JOHN@Example.COM  "));

    assertNotNull(user.id());
    assertEquals("john@example.com", user.email().value());
    assertEquals(UserStatus.PENDING_EMAIL_VERIFICATION, user.status());
    assertFalse(user.emailVerified());
    assertTrue(user.roles().contains(Role.CUSTOMER));
    assertEquals(AccountProfile.CUSTOMER, user.accountProfile());
    assertEquals(CustomerProfileType.INDIVIDUAL, user.customerProfileType());
  }

  @Test
  void shouldActivateUserWhenEmailIsVerified() {
    User user =
        new User(
            UUID.randomUUID(),
            new Email("john@example.com"),
            false,
            UserStatus.PENDING_EMAIL_VERIFICATION);

    User verifiedUser = user.verifyEmail();

    assertEquals(user.id(), verifiedUser.id());
    assertEquals(user.email(), verifiedUser.email());
    assertEquals(UserStatus.ACTIVE, verifiedUser.status());
    assertTrue(verifiedUser.emailVerified());
  }

  @Test
  void shouldCreateAdminOnlyWithExplicitFactory() {
    User admin = User.admin(new Email("admin@example.com"));

    assertTrue(admin.roles().contains(Role.ADMIN));
    assertEquals(AccountProfile.ADMIN, admin.accountProfile());
    assertEquals(UserStatus.ACTIVE, admin.status());
  }

  @Test
  void shouldAddAdminRoleWithoutRemovingCustomerRole() {
    User user = User.register(new Email("john@example.com"));

    User adminUser = user.assignRole(Role.ADMIN);

    assertTrue(adminUser.roles().contains(Role.CUSTOMER));
    assertTrue(adminUser.roles().contains(Role.ADMIN));
    assertEquals(AccountProfile.ADMIN, adminUser.accountProfile());
  }

  @Test
  void shouldPreventSuspendedUserFromLogin() {
    User user = User.register(new Email("john@example.com"));

    User suspended = user.suspend();

    assertEquals(UserStatus.SUSPENDED, suspended.status());
    assertFalse(suspended.canLogin());
  }
}
