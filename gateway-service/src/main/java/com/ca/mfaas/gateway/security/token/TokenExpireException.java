package com.ca.mfaas.gateway.security.token;

public class TokenExpireException extends RuntimeException {
    public TokenExpireException(String message) {
        super(message);
    }
}
