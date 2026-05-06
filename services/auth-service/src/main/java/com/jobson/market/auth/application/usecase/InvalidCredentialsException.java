package com.jobson.market.auth.application.usecase;

public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Invalid credentials");
  }
}
