package com.jobson.market.seller_service.web;

import com.jobson.market.seller_service.application.SellerNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class SellerExceptionHandler {

  @ExceptionHandler(SellerNotFoundException.class)
  ResponseEntity<Map<String, String>> sellerNotFound(SellerNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("message", exception.getMessage()));
  }
}
