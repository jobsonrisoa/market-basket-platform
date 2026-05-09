package com.jobson.market.auth.infrastructure.web;

import com.jobson.market.auth.application.usecase.admin.ForbiddenUserManagementException;
import com.jobson.market.auth.application.usecase.admin.UserNotFoundException;
import com.jobson.market.auth.application.usecase.authentication.InvalidCredentialsException;
import com.jobson.market.auth.application.usecase.authentication.InvalidRefreshTokenException;
import com.jobson.market.auth.application.usecase.registration.DuplicateEmailException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class AuthExceptionHandler {

  private static final String ERROR_KEY = "error";

  @ExceptionHandler(DuplicateEmailException.class)
  ResponseEntity<Map<String, String>> duplicateEmail() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of(ERROR_KEY, "Email is already registered"));
  }

  @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
  ResponseEntity<Map<String, String>> unauthorized() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of(ERROR_KEY, "Invalid credentials"));
  }

  @ExceptionHandler(ForbiddenUserManagementException.class)
  ResponseEntity<Map<String, String>> forbidden() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(ERROR_KEY, "Forbidden"));
  }

  @ExceptionHandler(UserNotFoundException.class)
  ResponseEntity<Map<String, String>> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(ERROR_KEY, "User not found"));
  }

  @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
  ResponseEntity<Map<String, String>> badRequest() {
    return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Invalid request"));
  }
}
