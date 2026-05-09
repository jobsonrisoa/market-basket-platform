package com.jobson.market.auth.application.usecase.authentication;

public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Invalid credentials");
  }
}
