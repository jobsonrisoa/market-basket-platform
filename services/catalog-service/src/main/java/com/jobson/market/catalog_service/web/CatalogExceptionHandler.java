package com.jobson.market.catalog_service.web;

import com.jobson.market.catalog_service.application.CategoryNotFoundException;
import com.jobson.market.catalog_service.application.ProductNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class CatalogExceptionHandler {

  @ExceptionHandler({CategoryNotFoundException.class, ProductNotFoundException.class})
  ResponseEntity<Map<String, String>> handleNotFound(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", exception.getMessage()));
  }

  @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
  ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
    return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
  }
}
