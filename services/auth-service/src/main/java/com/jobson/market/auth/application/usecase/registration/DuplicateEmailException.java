package com.jobson.market.auth.application.usecase.registration;

public class DuplicateEmailException extends RuntimeException {

  public DuplicateEmailException() {
    super("Email is already registered");
  }
}
