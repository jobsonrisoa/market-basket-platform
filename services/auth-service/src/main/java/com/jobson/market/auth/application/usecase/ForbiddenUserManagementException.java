package com.jobson.market.auth.application.usecase;

public class ForbiddenUserManagementException extends RuntimeException {

  public ForbiddenUserManagementException() {
    super("User is not allowed to manage roles");
  }
}
