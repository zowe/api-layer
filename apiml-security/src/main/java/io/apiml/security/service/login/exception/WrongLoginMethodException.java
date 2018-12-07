package io.apiml.security.service.login.exception;

import org.springframework.security.core.AuthenticationException;

public class WrongLoginMethodException extends AuthenticationException {
    public WrongLoginMethodException(String message) {
        super(message);
    }
}
