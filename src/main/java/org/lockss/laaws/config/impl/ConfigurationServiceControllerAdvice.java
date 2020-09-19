package org.lockss.laaws.config.impl;

import org.lockss.spring.error.RestResponseErrorBody;
import org.lockss.spring.error.SpringControllerAdvice;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ConfigurationServiceControllerAdvice extends SpringControllerAdvice {

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<RestResponseErrorBody> handler(final HttpMediaTypeNotAcceptableException e) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    return new ResponseEntity<>(new RestResponseErrorBody(e.getMessage(), e.getClass().getSimpleName()), headers,
        HttpStatus.NOT_ACCEPTABLE);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<RestResponseErrorBody> handler(final HttpMediaTypeNotSupportedException e) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    return new ResponseEntity<>(new RestResponseErrorBody(e.getMessage(), e.getClass().getSimpleName()), headers,
        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<RestResponseErrorBody> handler(final HttpRequestMethodNotSupportedException e) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    return new ResponseEntity<>(new RestResponseErrorBody(e.getMessage(), e.getClass().getSimpleName()), headers,
        HttpStatus.METHOD_NOT_ALLOWED);
  }

}
