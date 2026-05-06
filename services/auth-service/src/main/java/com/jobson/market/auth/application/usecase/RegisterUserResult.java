package com.jobson.market.auth.application.usecase;

import java.util.UUID;

public record RegisterUserResult(UUID userId, String email) {}
