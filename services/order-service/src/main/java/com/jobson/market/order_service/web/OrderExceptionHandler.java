package com.jobson.market.order_service.web;

import com.jobson.market.order_service.application.InventoryReservationFailedException;
import com.jobson.market.order_service.application.OrderNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class OrderExceptionHandler {

  private static final String ERROR_KEY = "error";

  @ExceptionHandler(OrderNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  Map<String, String> notFound(RuntimeException exception) {
    return Map.of(ERROR_KEY, exception.getMessage());
  }

  @ExceptionHandler(InventoryReservationFailedException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  Map<String, String> inventoryFailure(RuntimeException exception) {
    return Map.of(ERROR_KEY, exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  Map<String, String> validationFailure() {
    return Map.of(ERROR_KEY, "Invalid order request");
  }
}
