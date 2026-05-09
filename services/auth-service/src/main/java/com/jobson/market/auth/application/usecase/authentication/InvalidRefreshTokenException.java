package com.jobson.market.auth.application.usecase.authentication;

public class InvalidRefreshTokenException extends RuntimeException {

  public InvalidRefreshTokenException() {
    super("Invalid refresh token");
  }
}
