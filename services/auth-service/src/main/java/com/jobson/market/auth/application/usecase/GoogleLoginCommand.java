package com.jobson.market.auth.application.usecase;

public record GoogleLoginCommand(String subject, String email, boolean emailVerified) {}
