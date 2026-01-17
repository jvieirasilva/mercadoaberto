package com.security.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * ✅ Captura RuntimeException e retorna JSON com mensagem
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        LOGGER.error("RuntimeException: {}", ex.getMessage());
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * ✅ Captura exceções genéricas
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        LOGGER.error("Exception: {}", ex.getMessage());
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "An unexpected error occurred");
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}