package com.jobson.market.auth.application.usecase.authentication;

public record LoginWithPasswordCommand(String email, String password) {}
