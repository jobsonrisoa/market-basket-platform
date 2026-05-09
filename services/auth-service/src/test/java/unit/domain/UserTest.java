package unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.auth.domain.model.AccountProfile;
import com.jobson.market.auth.domain.model.CustomerProfileType;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.Permission;
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
    assertEquals(AccountProfile.PLATFORM, admin.accountProfile());
    assertEquals(UserStatus.ACTIVE, admin.status());
  }

  @Test
  void shouldAddAdminRoleWithoutRemovingCustomerRole() {
    User user = User.register(new Email("john@example.com"));

    User adminUser = user.assignRole(Role.ADMIN);

    assertTrue(adminUser.roles().contains(Role.CUSTOMER));
    assertTrue(adminUser.roles().contains(Role.ADMIN));
    assertEquals(AccountProfile.PLATFORM, adminUser.accountProfile());
    assertTrue(adminUser.permissions().contains(Permission.AUTH_USER_ROLE_ASSIGN));
  }

  @Test
  void shouldMapSellerRolesToSellerProfileAndPermissions() {
    User user = User.register(new Email("seller@example.com"));

    User seller = user.assignRole(Role.SELLER_OWNER);

    assertTrue(seller.roles().contains(Role.CUSTOMER));
    assertTrue(seller.roles().contains(Role.SELLER_OWNER));
    assertEquals(AccountProfile.SELLER, seller.accountProfile());
    assertTrue(seller.permissions().contains(Permission.SELLER_CATALOG_MANAGE));
    assertTrue(seller.permissions().contains(Permission.SELLER_STAFF_MANAGE));
  }

  @Test
  void shouldCreateSuperAdminProfile() {
    User user = User.superAdmin(new Email("owner@example.com"));

    assertTrue(user.roles().contains(Role.SUPER_ADMIN));
    assertEquals(AccountProfile.PLATFORM, user.accountProfile());
    assertTrue(user.permissions().contains(Permission.AUTH_USER_ROLE_REVOKE));
  }

  @Test
  void shouldPreventSuspendedUserFromLogin() {
    User user = User.register(new Email("john@example.com"));

    User suspended = user.suspend();

    assertEquals(UserStatus.SUSPENDED, suspended.status());
    assertFalse(suspended.canLogin());
  }
}
