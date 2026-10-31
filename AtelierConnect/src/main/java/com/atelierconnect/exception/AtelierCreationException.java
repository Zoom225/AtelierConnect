package com.atelierconnect.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AtelierCreationException extends RuntimeException {

    public AtelierCreationException(String message) {
        super(message);
    }
}
