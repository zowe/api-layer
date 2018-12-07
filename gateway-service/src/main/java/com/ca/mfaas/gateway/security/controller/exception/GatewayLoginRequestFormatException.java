package com.ca.mfaas.gateway.security.controller.exception;

public class GatewayLoginRequestFormatException extends RuntimeException {
    public GatewayLoginRequestFormatException(String message) {
        super(message);
    }
}
