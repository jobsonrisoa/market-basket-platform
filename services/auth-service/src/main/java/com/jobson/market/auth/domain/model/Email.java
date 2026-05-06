package com.jobson.market.auth.domain.model;

import java.util.regex.Pattern;

public record Email(String value) {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

  public Email {
    if (value != null) {
      value = value.trim().toLowerCase();
    }
    if (value == null || value.isBlank() || !EMAIL_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid email");
    }
  }
}
