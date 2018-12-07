package io.apiml.security.service.query;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class ServiceCookieTokenFilter extends AbstractSecureContentFilter {
    private static final String COOKIE_NAME = "apimlAuthenticationToken";

    public ServiceCookieTokenFilter(AuthenticationManager authenticationManager,
                                    AuthenticationFailureHandler failureHandler) {
        super(authenticationManager, failureHandler);
    }

    @Override
    protected String extractContent(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(COOKIE_NAME)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
