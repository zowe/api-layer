package io.apiml.security.service.query;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;

public class ServiceHeaderTokenFilter extends AbstractSecureContentFilter {
    public ServiceHeaderTokenFilter(
        AuthenticationManager authenticationManager,
        AuthenticationFailureHandler failureHandler) {
        super(authenticationManager, failureHandler);
    }

    @Override
    protected String extractContent(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            return token.replaceFirst("Bearer ", "");
        }
        return null;
    }
}
