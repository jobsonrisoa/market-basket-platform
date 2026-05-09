package com.jobson.market.auth.application.usecase.authentication;

public record LoginWithPasswordResult(String accessToken, String refreshToken) {}
