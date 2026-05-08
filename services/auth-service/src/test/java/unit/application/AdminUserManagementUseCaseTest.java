package unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.auth.application.port.event.OutboxEventRepository;
import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.application.usecase.AdminUserManagementUseCase;
import com.jobson.market.auth.application.usecase.AssignRoleCommand;
import com.jobson.market.auth.application.usecase.ForbiddenUserManagementException;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.Role;
import com.jobson.market.auth.domain.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminUserManagementUseCaseTest {

  @Test
  void shouldAllowAdminToPromoteCustomerToAdmin() {
    User admin = User.admin(new Email("admin@example.com"));
    User customer = User.register(new Email("customer@example.com"));
    FakeUserRepository users = new FakeUserRepository(admin, customer);
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    AdminUserManagementUseCase useCase = new AdminUserManagementUseCase(users, outbox);

    User updated = useCase.assignRole(new AssignRoleCommand(admin.id(), customer.id(), Role.ADMIN));

    assertTrue(updated.roles().contains(Role.ADMIN));
    assertTrue(updated.roles().contains(Role.CUSTOMER));
    assertEquals(1, outbox.events.size());
    assertEquals("auth.user.role_assigned.v1", outbox.events.get(0).eventType());
  }

  @Test
  void shouldRejectCustomerPromotingAnotherUser() {
    User actor = User.register(new Email("actor@example.com"));
    User target = User.register(new Email("target@example.com"));
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    AdminUserManagementUseCase useCase =
        new AdminUserManagementUseCase(new FakeUserRepository(actor, target), outbox);

    assertThrows(
        ForbiddenUserManagementException.class,
        () -> useCase.assignRole(new AssignRoleCommand(actor.id(), target.id(), Role.ADMIN)));

    assertTrue(outbox.events.isEmpty());
  }

  private static class FakeUserRepository implements UserRepository {
    private final Map<UUID, User> usersById = new HashMap<>();

    private FakeUserRepository(User... users) {
      for (User user : users) {
        usersById.put(user.id(), user);
      }
    }

    @Override
    public boolean existsByEmail(Email email) {
      return usersById.values().stream().anyMatch(user -> user.email().equals(email));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
      return usersById.values().stream().filter(user -> user.email().equals(email)).findFirst();
    }

    @Override
    public Optional<User> findById(UUID id) {
      return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public User save(User user) {
      usersById.put(user.id(), user);
      return user;
    }
  }

  private static class FakeOutboxEventRepository implements OutboxEventRepository {
    private final List<OutboxEvent> events = new ArrayList<>();

    @Override
    public void save(OutboxEvent event) {
      events.add(event);
    }
  }
}
