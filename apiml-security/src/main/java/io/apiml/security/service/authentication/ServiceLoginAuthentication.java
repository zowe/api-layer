package io.apiml.security.service.authentication;

import io.apiml.security.service.login.dto.ServiceLoginRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class ServiceLoginAuthentication implements Authentication {
    private final String username;
    private final String password;

    public ServiceLoginAuthentication(ServiceLoginRequest loginRequest) {
        this.username = loginRequest.getUsername();
        this.password = loginRequest.getPassword();
    }

    public ServiceLoginAuthentication(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getCredentials() {
        return password;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public String getPrincipal() {
        return username;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    }

    @Override
    public String getName() {
        return username;
    }
}
