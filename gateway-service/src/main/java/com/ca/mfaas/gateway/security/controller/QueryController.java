package com.ca.mfaas.gateway.security.controller;

import com.ca.mfaas.gateway.security.controller.exception.QueryRequestException;
import com.ca.mfaas.gateway.security.token.TokenService;
import io.apiml.security.gateway.query.QueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/auth/query")
public class QueryController {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TYPE = "Bearer ";
    private final TokenService tokenService;

    public QueryController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping(
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public QueryResponse query(HttpServletRequest req) {
        String authorization = req.getHeader(AUTHORIZATION_HEADER);
        validateHeader(authorization);
        String token = authorization.replace(BEARER_TYPE, "");
        return tokenService.validate(token);
    }

    private void validateHeader(String authorization) {
        if (StringUtils.isBlank(authorization)) {
            throw new QueryRequestException("Token is blank or missing");
        }

        if (!authorization.startsWith(BEARER_TYPE)) {
            throw new QueryRequestException("Token format is wrong");
        }

    }
}
