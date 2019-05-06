package com.ca.apiml.security.content;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.token.TokenAuthentication;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import java.util.Optional;

public class CookieContentFilterTest {

    private CookieContentFilter cookieContentFilter;
    private AuthenticationManager authenticationManager;
    private AuthenticationFailureHandler failureHandler;
    private SecurityConfigurationProperties securityConfigurationProperties;
    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        failureHandler = mock(AuthenticationFailureHandler.class);
        securityConfigurationProperties = new SecurityConfigurationProperties();
        request = new MockHttpServletRequest();
        cookieContentFilter = new CookieContentFilter(authenticationManager, failureHandler, securityConfigurationProperties);
    }

    @Test
    public void shouldReturnEmptyIfNoCookies() {
        Optional<AbstractAuthenticationToken> content =  cookieContentFilter.extractContent(request);
        assertEquals(Optional.empty(), content);
    }

    @Test
    public void shouldExtractContent() {
        Cookie cookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), "cookie");
        request.setCookies(cookie);
        Optional<AbstractAuthenticationToken> content =  cookieContentFilter.extractContent(request);
        TokenAuthentication actualToken = new TokenAuthentication(cookie.getValue());
        assertTrue(content.isPresent());
        assertEquals(content.get(), actualToken);

    }

}
