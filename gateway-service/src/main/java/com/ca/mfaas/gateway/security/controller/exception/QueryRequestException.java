package com.ca.mfaas.gateway.security.controller.exception;

public class QueryRequestException extends RuntimeException {
    public QueryRequestException(String message) {
        super(message);
    }
}
