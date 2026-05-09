package com.jobson.market.auth.application.usecase.admin;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException() {
    super("User not found");
  }
}
