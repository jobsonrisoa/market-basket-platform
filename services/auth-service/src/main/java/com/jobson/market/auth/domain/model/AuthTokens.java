package com.jobson.market.auth.domain.model;

public record AuthTokens(String accessToken, String refreshToken) {

  public AuthTokens {
    if (accessToken == null || accessToken.isBlank()) {
      throw new IllegalArgumentException("accessToken is required");
    }
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("refreshToken is required");
    }
  }
}
