package com.jobson.market.auth.infrastructure.web;

import com.jobson.market.auth.application.usecase.DuplicateEmailException;
import com.jobson.market.auth.application.usecase.ForbiddenUserManagementException;
import com.jobson.market.auth.application.usecase.InvalidCredentialsException;
import com.jobson.market.auth.application.usecase.InvalidRefreshTokenException;
import com.jobson.market.auth.application.usecase.UserNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class AuthExceptionHandler {

  @ExceptionHandler(DuplicateEmailException.class)
  ResponseEntity<Map<String, String>> duplicateEmail() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "Email is already registered"));
  }

  @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
  ResponseEntity<Map<String, String>> unauthorized() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "Invalid credentials"));
  }

  @ExceptionHandler(ForbiddenUserManagementException.class)
  ResponseEntity<Map<String, String>> forbidden() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
  }

  @ExceptionHandler(UserNotFoundException.class)
  ResponseEntity<Map<String, String>> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
  }

  @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
  ResponseEntity<Map<String, String>> badRequest() {
    return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
  }
}
