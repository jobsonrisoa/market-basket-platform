package com.jobson.market.auth.application.usecase.registration;

import com.jobson.market.auth.domain.model.Role;
import java.util.Set;
import java.util.UUID;

public record RegisterUserResult(UUID userId, String email, Set<Role> roles) {}
