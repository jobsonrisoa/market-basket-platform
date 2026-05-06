package com.jobson.market.auth.application.usecase;

public record LoginWithPasswordCommand(String email, String password) {}
