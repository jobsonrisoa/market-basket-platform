package com.jobson.market.inventory_service.web;

import com.jobson.market.inventory_service.application.InventoryNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class InventoryExceptionHandler {

  @ExceptionHandler(InventoryNotFoundException.class)
  ResponseEntity<Map<String, String>> inventoryNotFound(InventoryNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("message", exception.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, String>> invalidInventoryRequest(IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("message", exception.getMessage()));
  }
}
