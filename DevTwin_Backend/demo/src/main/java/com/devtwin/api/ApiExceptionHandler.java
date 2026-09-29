package com.devtwin.api;

import com.devtwin.connector.github.GithubApiException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(GithubApiException.class)
    public ResponseEntity<ApiError> handleGithubApiException(GithubApiException exception) {
        HttpStatus status = exception.getStatusCode() == 404
                ? HttpStatus.NOT_FOUND
                : exception.getStatusCode() == 429
                    ? HttpStatus.TOO_MANY_REQUESTS
                    : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(new ApiError(status.value(), exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(400, exception.getMessage(), Instant.now()));
    }

    public record ApiError(int status, String message, Instant timestamp) {
    }
}
