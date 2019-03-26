package com.ca.apiml.security.content;

import com.ca.apiml.security.token.TokenAuthentication;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class CookieFilter extends OncePerRequestFilter {
    private final AuthenticationManager authenticationManager;
    private final AuthenticationFailureHandler failureHandler;

    public CookieFilter(AuthenticationManager authenticationManager, AuthenticationFailureHandler failureHandler) {
        this.authenticationManager = authenticationManager;
        this.failureHandler = failureHandler;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        TokenAuthentication authenticationToken = extractContent(request);

        if (authenticationToken != null) {
            try {
                Authentication authentication = authenticationManager.authenticate(authenticationToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
            } catch (AuthenticationException authenticationException) {
                failureHandler.onAuthenticationFailure(request, response, authenticationException);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private TokenAuthentication extractContent(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("apimlAuthenticationToken")) {
                return new TokenAuthentication(cookie.getValue());
            }
        }

        return null;
    }
}
