package com.broadcom.apiml.library.service.security.security.service.security;

import com.broadcom.apiml.library.service.security.security.service.service.TokenService;
import com.broadcom.apiml.library.service.security.security.service.service.TokenValidationResult;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

@WebFilter("/sso/token/validate")
public class JwtTokenFilter implements Filter {
    private final TokenService tokenService;

    public JwtTokenFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        TokenValidationResult validationResult = tokenService.validateRequestWithToken((HttpServletRequest) request);
        if (validationResult.isValid()) {
            chain.doFilter(request, response);
        } else {
            throw new IOException("Token is not Valid");
        }

    }

    @Override
    public void destroy() {}
}
