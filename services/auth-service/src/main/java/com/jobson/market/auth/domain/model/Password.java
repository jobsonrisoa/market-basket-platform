package com.jobson.market.auth.domain.model;

public record Password(String value) {

  private static final int MIN_LENGTH = 12;

  public Password {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Password is required");
    }
    if (!isStrong(value)) {
      throw new IllegalArgumentException("Password does not meet security policy");
    }
  }

  private static boolean isStrong(String value) {
    return value.length() >= MIN_LENGTH
        && value.chars().anyMatch(Character::isUpperCase)
        && value.chars().anyMatch(Character::isLowerCase)
        && value.chars().anyMatch(Character::isDigit)
        && value.chars().anyMatch(Password::isSpecialCharacter);
  }

  private static boolean isSpecialCharacter(int character) {
    return !Character.isLetterOrDigit(character) && !Character.isWhitespace(character);
  }
}
