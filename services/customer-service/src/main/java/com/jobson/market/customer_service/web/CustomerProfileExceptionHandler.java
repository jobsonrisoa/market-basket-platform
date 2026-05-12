package com.jobson.market.customer_service.web;

import com.jobson.market.customer_service.application.CustomerProfileNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class CustomerProfileExceptionHandler {

  @ExceptionHandler(CustomerProfileNotFoundException.class)
  ResponseEntity<Map<String, String>> customerProfileNotFound(
      CustomerProfileNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("message", exception.getMessage()));
  }
}
