package com.webworks.alert_case_service.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)   // Week 4 replaces this with a ProblemDetail handler
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}