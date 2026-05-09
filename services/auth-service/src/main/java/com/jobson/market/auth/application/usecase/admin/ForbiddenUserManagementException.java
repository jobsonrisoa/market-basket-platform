package com.jobson.market.auth.application.usecase.admin;

public class ForbiddenUserManagementException extends RuntimeException {

  public ForbiddenUserManagementException() {
    super("User is not allowed to manage roles");
  }
}
