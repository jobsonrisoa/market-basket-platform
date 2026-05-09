package com.jobson.market.auth.application.usecase.admin;

import com.jobson.market.auth.application.port.event.OutboxEventRepository;
import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.Permission;
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
    User actor = requirePermission(command.actorUserId(), Permission.AUTH_USER_ROLE_ASSIGN);
    requireSuperAdminForSensitiveRole(actor, command.role());
    User updated = users.save(findUser(command.targetUserId()).assignRole(command.role()));
    outbox.save(OutboxEvent.roleAssigned(updated, command.role(), command.actorUserId()));
    return updated;
  }

  @Transactional
  public User removeRole(RemoveRoleCommand command) {
    User actor = requirePermission(command.actorUserId(), Permission.AUTH_USER_ROLE_REVOKE);
    requireSuperAdminForSensitiveRole(actor, command.role());
    User updated = users.save(findUser(command.targetUserId()).removeRole(command.role()));
    outbox.save(OutboxEvent.roleRemoved(updated, command.role(), command.actorUserId()));
    return updated;
  }

  @Transactional
  public User suspend(UUID actorUserId, UUID targetUserId) {
    requirePermission(actorUserId, Permission.PLATFORM_SELLER_REVIEW);
    User updated = users.save(findUser(targetUserId).suspend());
    outbox.save(OutboxEvent.accountSuspended(updated, actorUserId));
    return updated;
  }

  @Transactional
  public User reactivate(UUID actorUserId, UUID targetUserId) {
    requirePermission(actorUserId, Permission.PLATFORM_SELLER_REVIEW);
    User updated = users.save(findUser(targetUserId).reactivate());
    outbox.save(OutboxEvent.accountReactivated(updated, actorUserId));
    return updated;
  }

  private User requirePermission(UUID actorUserId, Permission permission) {
    User actor = findUser(actorUserId);
    if (!actor.hasPermission(permission)) {
      throw new ForbiddenUserManagementException();
    }
    return actor;
  }

  private void requireSuperAdminForSensitiveRole(User actor, Role role) {
    if (role.isSecuritySensitive() && !actor.hasRole(Role.SUPER_ADMIN)) {
      throw new ForbiddenUserManagementException();
    }
  }

  private User findUser(UUID userId) {
    return users.findById(userId).orElseThrow(UserNotFoundException::new);
  }
}
