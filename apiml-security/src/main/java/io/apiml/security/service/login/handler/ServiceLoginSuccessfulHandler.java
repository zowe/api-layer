package io.apiml.security.service.login.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.apiml.security.service.authentication.ApimlAuthentication;
import io.apiml.security.service.login.dto.ServiceLoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class ServiceLoginSuccessfulHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper mapper;

    public ServiceLoginSuccessfulHandler(ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication)
        throws IOException {
        ApimlAuthentication apimlAuthentication = (ApimlAuthentication) authentication;
        String token = apimlAuthentication.getCredentials();
        response.addCookie(createCookie(token));
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.setStatus(HttpStatus.OK.value());
        mapper.writeValue(response.getWriter(), new ServiceLoginResponse(token));
    }

    private Cookie createCookie(String token) {
        Cookie tokenCookie = new Cookie("apimlAuthenticationToken", token);
        tokenCookie.setComment("API Mediation Layer Authentication Token");
        tokenCookie.setPath("/");
        tokenCookie.setHttpOnly(true);
        tokenCookie.setMaxAge(-1);
        tokenCookie.setSecure(true);

        return tokenCookie;
    }
}
