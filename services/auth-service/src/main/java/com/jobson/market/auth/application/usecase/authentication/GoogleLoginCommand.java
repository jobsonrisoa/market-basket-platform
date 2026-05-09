package com.jobson.market.auth.application.usecase.authentication;

public record GoogleLoginCommand(String subject, String email, boolean emailVerified) {}
