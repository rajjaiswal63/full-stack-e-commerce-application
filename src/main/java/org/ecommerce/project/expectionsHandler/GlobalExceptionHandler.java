package org.ecommerce.project.expectionsHandler;

import org.ecommerce.project.payload.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;

@Profile("legacy")
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>>MethodArgumentNotValidException(MethodArgumentNotValidException e){
        HashMap<String,String> result = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error)->{
            String fieldName =((FieldError)error).getField();
            String message =error.getDefaultMessage();
            result.put(fieldName,message);
        });
        return new ResponseEntity<Map<String,String>>(result, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse> ResourceNotFoundException(ResourceNotFoundException e){
        String message=e.getMessage();
        APIResponse apiResponse = new APIResponse(message,false);
        return new ResponseEntity<>(apiResponse,HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIResponse> APIException(APIException e){
        String message= e.getMessage();
        APIResponse apiResponse = new APIResponse(message,false);
        return new ResponseEntity<>(apiResponse,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoCategoryPresentExceptio.class)
    public ResponseEntity<APIResponse> NoCategoryPresentExceptio(NoCategoryPresentExceptio e){
        String message= e.getMessage();
        APIResponse apiResponse = new APIResponse(message,false);
        return new ResponseEntity<>(apiResponse,HttpStatus.BAD_REQUEST);
    }
}
