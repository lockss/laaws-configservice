package org.lockss.laaws.config.impl;

import org.lockss.spring.error.RestResponseErrorBody;
import org.lockss.spring.error.SpringControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ConfigurationServiceControllerAdvice extends SpringControllerAdvice {

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<RestResponseErrorBody> handler(final HttpMediaTypeNotAcceptableException e) {
    return new ResponseEntity<>(new RestResponseErrorBody(e.getMessage(), e.getClass().getSimpleName()),
        HttpStatus.NOT_ACCEPTABLE);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<RestResponseErrorBody> handler(final HttpRequestMethodNotSupportedException e) {
    return new ResponseEntity<>(new RestResponseErrorBody(e.getMessage(), e.getClass().getSimpleName()),
        HttpStatus.METHOD_NOT_ALLOWED);
  }

}
