package com.jobson.market.auth.application.usecase.admin;

import com.jobson.market.auth.domain.model.Role;
import java.util.UUID;

public record RemoveRoleCommand(UUID actorUserId, UUID targetUserId, Role role) {}
