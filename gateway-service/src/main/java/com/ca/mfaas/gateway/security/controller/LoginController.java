package com.ca.mfaas.gateway.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ca.mfaas.gateway.security.controller.exception.GatewayLoginRequestFormatException;
import com.ca.mfaas.gateway.security.token.TokenService;
import io.apiml.security.gateway.login.GatewayLoginRequest;
import io.apiml.security.gateway.login.GatewayLoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/auth/login")
public class LoginController {
    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String COOKIE_NAME = "apimlAuthenticationToken";

    private final ObjectMapper mapper;
    private final TokenService tokenService;

    public LoginController(ObjectMapper objectMapper, TokenService tokenService) {
        this.tokenService = tokenService;
        this.mapper = objectMapper;
    }

    @PostMapping(
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
        consumes = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public ResponseEntity<GatewayLoginResponse> processLoginRequest(HttpServletRequest req, HttpServletResponse res) {
        GatewayLoginRequest loginRequestFromHeader = makeLoginWithHeader(req);
        GatewayLoginRequest loginRequestFromBody = makeLoginWithBody(req);
        GatewayLoginResponse response = doLogin(loginRequestFromHeader, loginRequestFromBody);

        Cookie tokenCookie = new Cookie(COOKIE_NAME, response.getApimlAuthenticationToken());
        tokenCookie.setComment("API Mediation Layer Authentication Token");
        tokenCookie.setPath("/");
        tokenCookie.setHttpOnly(true);
        tokenCookie.setMaxAge(-1);
        tokenCookie.setSecure(true);
        res.addCookie(tokenCookie);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response);
    }

    private GatewayLoginResponse doLogin(GatewayLoginRequest loginRequestFromHeader, GatewayLoginRequest loginRequestFromBody) {
        GatewayLoginResponse response = null;
        if (loginRequestFromHeader != null && loginRequestFromBody != null) {
            throw new GatewayLoginRequestFormatException("Use only header or body for authentication");
        } else {
            if (loginRequestFromBody == null && loginRequestFromHeader == null) {
                throw new GatewayLoginRequestFormatException("Login object has wrong format");
            } else {
                if (loginRequestFromHeader != null) {
                    response = tokenService.login(loginRequestFromHeader);
                } else {
                    response = tokenService.login(loginRequestFromBody);
                }
            }
        }
        return response;
    }

    private GatewayLoginRequest makeLoginWithBody(HttpServletRequest request) {
        GatewayLoginRequest loginRequest = null;
        try {
            loginRequest = mapper.readValue(request.getInputStream(), GatewayLoginRequest.class);
            if (StringUtils.isBlank(loginRequest.getUsername()) || StringUtils.isBlank(loginRequest.getPassword())) {
                return null;
            } else {
                return loginRequest;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private GatewayLoginRequest makeLoginWithHeader(HttpServletRequest req) {
        GatewayLoginRequest loginResponseFromHeader = null;
        try {
            String authorization = req.getHeader("Authorization");
            if (authorization != null) {
                if (authorization.startsWith(BASIC_AUTH_PREFIX)) {
                    String base64Credentials = authorization.substring(BASIC_AUTH_PREFIX.length()).trim();
                    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                    String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                    final String[] values = credentials.split(":", 2);
                    loginResponseFromHeader = new GatewayLoginRequest(values[0], values[1]);
                    return loginResponseFromHeader;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
