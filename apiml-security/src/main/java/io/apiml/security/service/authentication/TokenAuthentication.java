package io.apiml.security.service.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

public class TokenAuthentication extends AbstractAuthenticationToken {
    private final String token;

    public TokenAuthentication(String token) {
        super(Collections.emptyList());
        this.token = token;
    }

    @Override
    public String getCredentials() {
        return token;
    }

    @Override
    public String getPrincipal() {
        return token;
    }
}
