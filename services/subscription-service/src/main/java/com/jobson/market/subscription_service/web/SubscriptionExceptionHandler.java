package com.jobson.market.subscription_service.web;

import com.jobson.market.subscription_service.application.SubscriptionNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class SubscriptionExceptionHandler {

  @ExceptionHandler(SubscriptionNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  Map<String, String> notFound(RuntimeException exception) {
    return Map.of("error", exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  Map<String, String> validationFailure() {
    return Map.of("error", "Invalid subscription request");
  }
}
