package com.hacademy.video.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hacademy.video.auth.AuthenticationException;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> authentication(AuthenticationException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> invalidRequest(IllegalArgumentException e) {
		return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
	}

	public record ApiError(String message) {
	}
}
