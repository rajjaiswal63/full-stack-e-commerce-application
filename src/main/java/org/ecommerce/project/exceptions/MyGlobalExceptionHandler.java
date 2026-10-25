package org.ecommerce.project.exceptions;

import org.ecommerce.project.payload.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class MyGlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : "request";
            errors.put(fieldName, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse> handleNotFound(ResourceNotFoundException exception) {
        return new ResponseEntity<>(new APIResponse(exception.getMessage(), false), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIResponse> handleApiException(APIException exception) {
        return new ResponseEntity<>(new APIResponse(exception.getMessage(), false), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse> handleAccessDenied(AccessDeniedException exception) {
        return new ResponseEntity<>(new APIResponse("You do not have permission to perform this action.", false),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<APIResponse> handleAuthentication(AuthenticationException exception) {
        return new ResponseEntity<>(new APIResponse("Invalid username or password.", false),
                HttpStatus.UNAUTHORIZED);
    }
}
