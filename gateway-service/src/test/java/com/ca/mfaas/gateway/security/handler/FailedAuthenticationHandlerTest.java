package com.ca.mfaas.gateway.security.handler;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.error.impl.ErrorServiceImpl;
import com.ca.mfaas.gateway.security.token.TokenExpireException;
import com.ca.mfaas.gateway.security.token.TokenNotValidException;
import com.ca.mfaas.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
public class FailedAuthenticationHandlerTest {

    @Autowired
    private ErrorService errorService;

    @Mock
    private ObjectMapper objectMapper;

    private FailedAuthenticationHandler failedAuthenticationHandler;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;


    @Before
    public void setup() {
        failedAuthenticationHandler = new FailedAuthenticationHandler(errorService, objectMapper);
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRequestURI("URI");

        httpServletResponse = new MockHttpServletResponse();
    }

    @Test
    public void testAuthenticationFailureWhenExceptionIsBadCredentialsException() throws IOException {
        BadCredentialsException badCredentialsException = new BadCredentialsException("ERROR");
        failedAuthenticationHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, badCredentialsException);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.security.invalidUsername", badCredentialsException.getMessage(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }

    @Test
    public void testAuthenticationFailureWhenExceptionIsTokenNotValidException() throws IOException {
        TokenNotValidException tokenNotValidException = new TokenNotValidException("ERROR");
        failedAuthenticationHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, tokenNotValidException);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.security.tokenIsNotValid", tokenNotValidException.getMessage(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }

    @Test
    public void testAuthenticationFailureWhenOccurUnexpectedException() throws IOException {
        TokenExpireException tokenExpireException = new TokenExpireException("ERROR");
        failedAuthenticationHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, tokenExpireException);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.security.authenticationException", tokenExpireException.getMessage(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }


    @Configuration
    static class ContextConfiguration {
        @Bean
        public ErrorService errorService() {
            return new ErrorServiceImpl("/gateway-messages.yml");
        }
    }

}
