package com.jobson.market.auth.application.usecase;

import com.jobson.market.auth.application.port.event.OutboxEventRepository;
import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.Role;
import com.jobson.market.auth.domain.model.User;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class AdminUserManagementUseCase {

  private final UserRepository users;
  private final OutboxEventRepository outbox;

  public AdminUserManagementUseCase(UserRepository users, OutboxEventRepository outbox) {
    this.users = Objects.requireNonNull(users);
    this.outbox = Objects.requireNonNull(outbox);
  }

  @Transactional
  public User assignRole(AssignRoleCommand command) {
    requireAdmin(command.actorUserId());
    User updated = users.save(findUser(command.targetUserId()).assignRole(command.role()));
    outbox.save(OutboxEvent.roleAssigned(updated, command.role(), command.actorUserId()));
    return updated;
  }

  @Transactional
  public User removeRole(RemoveRoleCommand command) {
    requireAdmin(command.actorUserId());
    User updated = users.save(findUser(command.targetUserId()).removeRole(command.role()));
    outbox.save(OutboxEvent.roleRemoved(updated, command.role(), command.actorUserId()));
    return updated;
  }

  @Transactional
  public User suspend(UUID actorUserId, UUID targetUserId) {
    requireAdmin(actorUserId);
    User updated = users.save(findUser(targetUserId).suspend());
    outbox.save(OutboxEvent.accountSuspended(updated, actorUserId));
    return updated;
  }

  @Transactional
  public User reactivate(UUID actorUserId, UUID targetUserId) {
    requireAdmin(actorUserId);
    User updated = users.save(findUser(targetUserId).reactivate());
    outbox.save(OutboxEvent.accountReactivated(updated, actorUserId));
    return updated;
  }

  private void requireAdmin(UUID actorUserId) {
    User actor = findUser(actorUserId);
    if (!actor.hasRole(Role.ADMIN)) {
      throw new ForbiddenUserManagementException();
    }
  }

  private User findUser(UUID userId) {
    return users.findById(userId).orElseThrow(UserNotFoundException::new);
  }
}
