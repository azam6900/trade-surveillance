package com.webworks.alert_case_service.web;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}