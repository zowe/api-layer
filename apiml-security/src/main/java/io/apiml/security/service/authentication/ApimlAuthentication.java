package io.apiml.security.service.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

public class ApimlAuthentication extends AbstractAuthenticationToken {
    private final String username;
    private final String token;

    public ApimlAuthentication(String username, String token) {
        super(Collections.emptyList());
        this.username = username;
        this.token = token;
    }

    @Override
    public String getCredentials() {
        return token;
    }

    @Override
    public String getPrincipal() {
        return username;
    }
}
