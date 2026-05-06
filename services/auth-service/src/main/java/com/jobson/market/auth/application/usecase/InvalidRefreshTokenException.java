package com.jobson.market.auth.application.usecase;

public class InvalidRefreshTokenException extends RuntimeException {

  public InvalidRefreshTokenException() {
    super("Invalid refresh token");
  }
}
