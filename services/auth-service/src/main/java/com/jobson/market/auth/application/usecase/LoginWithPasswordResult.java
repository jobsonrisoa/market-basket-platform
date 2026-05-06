package com.jobson.market.auth.application.usecase;

public record LoginWithPasswordResult(String accessToken, String refreshToken) {}
