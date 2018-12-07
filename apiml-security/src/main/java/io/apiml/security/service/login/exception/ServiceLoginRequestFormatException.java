package io.apiml.security.service.login.exception;

import org.springframework.security.core.AuthenticationException;

public class ServiceLoginRequestFormatException extends AuthenticationException {
    public ServiceLoginRequestFormatException(String message) {
        super(message);
    }
}
