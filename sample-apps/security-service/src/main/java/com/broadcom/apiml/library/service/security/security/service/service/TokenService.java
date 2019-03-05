package com.broadcom.apiml.library.service.security.security.service.service;

import javax.servlet.http.HttpServletRequest;


public class TokenService {
    //private final UserService userService;

    public TokenService(UserService userService) {
        //this.userService = userService;
    }


    public TokenValidationResult validateRequestWithToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token != null) {
            return new TokenValidationResult(true);
        } else {
            throw new RuntimeException("token is not valid");
        }
    }

    String extractTokenFromRequest(HttpServletRequest request) {
        return "token for path: " + request.getPathInfo();
    }
}
