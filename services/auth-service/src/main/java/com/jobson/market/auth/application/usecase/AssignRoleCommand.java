package com.jobson.market.auth.application.usecase;

import com.jobson.market.auth.domain.model.Role;
import java.util.UUID;

public record AssignRoleCommand(UUID actorUserId, UUID targetUserId, Role role) {}
