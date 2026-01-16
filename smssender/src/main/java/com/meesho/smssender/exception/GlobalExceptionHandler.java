package com.meesho.smssender.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.meesho.smssender.dto.SmsResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<SmsResponse> handleValidationException(ValidationException ex) {
        return new ResponseEntity<SmsResponse>(
            new SmsResponse("FAILED", ex.getMessage()), org.springframework.http.HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(BlockedNumberException.class)
    public ResponseEntity<SmsResponse> handleBlockedNumberException(BlockedNumberException ex) {
        return new ResponseEntity<SmsResponse>(
            new SmsResponse("BLOCKED", ex.getMessage()), org.springframework.http.HttpStatus.FORBIDDEN
        );
    }
}
