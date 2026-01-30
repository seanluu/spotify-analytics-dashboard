package com.spotify.dashboard.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        logger.warn("Validation error: {}", message);
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientError(HttpClientErrorException e) {
        logger.error("HTTP client error: Status {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        
        // try to extract error message from response body, fallback to default
        String message = extractErrorMessage(e);
        
        return ResponseEntity.status(e.getStatusCode())
            .body(new ErrorResponse("EXTERNAL_API_ERROR", message));
    }
    
    private String extractErrorMessage(HttpClientErrorException e) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> errorBody = e.getResponseBodyAs(Map.class);
            if (errorBody != null && errorBody.containsKey("error")) {
                return errorBody.get("error").toString();
            }
        } catch (Exception ignored) {
        }
        return e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty()
            ? e.getResponseBodyAsString() 
            : "External API error";
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClientError(RestClientException e) {
        logger.error("Rest client error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse("SERVICE_UNAVAILABLE", "Failed to communicate with external service: " + e.getMessage()));
    }

    // catches all remaining exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        logger.error("Unexpected error: {}", e.getMessage(), e);
        // preserve the actual error message for debugging
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An unexpected error occurred. Please try again later.";
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", message));
    }

    public record ErrorResponse(
        String error,
        String message,
        LocalDateTime timestamp
    ) {
        public ErrorResponse(String error, String message) {
            this(error, message, LocalDateTime.now());
        }
    }
}

