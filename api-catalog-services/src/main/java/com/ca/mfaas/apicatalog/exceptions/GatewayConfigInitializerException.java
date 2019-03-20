package com.ca.mfaas.apicatalog.exceptions;

public class GatewayConfigInitializerException extends Exception {
    private static final long serialVersionUID = -559112794280136165L;

    public GatewayConfigInitializerException(String message) {
        super(message);
    }

    public GatewayConfigInitializerException(String message, Throwable cause) {
        super(message, cause);
    }
}
