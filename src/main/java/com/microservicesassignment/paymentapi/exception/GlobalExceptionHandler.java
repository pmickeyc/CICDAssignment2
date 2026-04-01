package com.microservicesassignment.paymentapi.exception;

import com.microservicesassignment.paymentapi.dto.ApiErrorResponse;
import com.microservicesassignment.paymentapi.dto.FieldValidationError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                                                    HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex,
                                                                                    HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidRequestException ex,
                                                                                          HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                                                      HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldError)
            .toList();

        return buildError(HttpStatus.BAD_REQUEST, "validation failed", request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                                               HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleTypeMismatch(Exception ex,
                                                                                        HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "invalid request format", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex,
                                                                                      HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected server error", request.getRequestURI(), List.of());
    }

    private FieldValidationError toFieldError(FieldError fieldError) {
        return new FieldValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private org.springframework.http.ResponseEntity<ApiErrorResponse> buildError(HttpStatus status,
                                                                                 String message,
                                                                                 String path,
                                                                                 List<FieldValidationError> fieldErrors) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setTimestamp(OffsetDateTime.now());
        error.setStatus(status.value());
        error.setError(status.getReasonPhrase());
        error.setMessage(message);
        error.setPath(path);
        error.setFieldErrors(fieldErrors);

        return new org.springframework.http.ResponseEntity<>(error, status);
    }
}
