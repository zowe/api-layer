package com.ca.mfaas.gateway.security.service.zosmf;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import com.ca.apiml.security.common.error.ServiceNotAccessibleException;
import com.ca.apiml.security.common.token.TokenNotValidException;
import com.ca.mfaas.gateway.security.service.ZosmfService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZosmfServiceV2Test {

    private static final String ZOSMF_ID = "zosmf";

    @Mock
    private AuthConfigurationProperties authConfigurationProperties;

    @Mock
    private DiscoveryClient discovery;

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper securityObjectMapper;

    @InjectMocks
    private ZosmfServiceV2 zosmfService;

    @Before
    public void setUp() {
        when(authConfigurationProperties.validatedZosmfServiceId()).thenReturn(ZOSMF_ID);
    }

    @Test(expected = ServiceNotAccessibleException.class)
    public void testInvalidateNoZosmf() {
        zosmfService.invalidate(ZosmfServiceV2.TokenType.JWT, "token");
    }

    private void mockZosmfService(String hostname, int port) {
        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getHostName()).thenReturn(hostname);
        when(instanceInfo.getPort()).thenReturn(port);
        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(Arrays.asList(instanceInfo));
        when(discovery.getApplication(ZOSMF_ID)).thenReturn(application);
    }

    @Test
    public void testInvalidateJwt() {
        mockZosmfService("zosmf", 1433);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-CSRF-ZOSMF-HEADER", "");
        headers.add(HttpHeaders.COOKIE, "jwtToken=x.y.z");

        when(restTemplate.exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.DELETE,
            new HttpEntity<>(null, headers),
            String.class
        )).thenReturn(new ResponseEntity(HttpStatus.OK));

        zosmfService.invalidate(ZosmfServiceV2.TokenType.JWT, "x.y.z");
        verify(restTemplate, times(1)).
            exchange(anyString(), (HttpMethod) any(), (HttpEntity) any(), (Class<?>) any());
    }

    @Test
    public void testInvalidateLtpa() {
        mockZosmfService("zosmf", 1433);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-CSRF-ZOSMF-HEADER", "");
        headers.add(HttpHeaders.COOKIE, "LtpaToken2=ltpa");

        when(restTemplate.exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.DELETE,
            new HttpEntity<>(null, headers),
            String.class
        )).thenReturn(new ResponseEntity(HttpStatus.OK));

        zosmfService.invalidate(ZosmfServiceV2.TokenType.LTPA, "ltpa");
        verify(restTemplate, times(1)).
            exchange(anyString(), (HttpMethod) any(), (HttpEntity) any(), (Class<?>) any());
    }

    @Test
    public void testInvalidateError500() {
        mockZosmfService("zosmf", 1433);
        when(restTemplate.exchange(anyString(), (HttpMethod) any(), (HttpEntity) any(), (Class<?>) any()))
            .thenReturn(new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR));

        try {
            zosmfService.invalidate(ZosmfServiceV2.TokenType.LTPA, "ltpa");
            fail();
        } catch (ServiceNotAccessibleException e) {
            assertEquals("Could not get an access to z/OSMF service.", e.getMessage());
        }
    }

    @Test
    public void testInvalidateException() {
        mockZosmfService("zosmf", 1433);

        try {
            zosmfService.invalidate(ZosmfServiceV2.TokenType.LTPA, "ltpa");
            fail();
        } catch (NullPointerException e) {
            // response is null
        }

        when(restTemplate.exchange(anyString(), (HttpMethod) any(), (HttpEntity) any(), (Class<?>) any()))
            .thenThrow(new ResourceAccessException("msg"));

        try {
            zosmfService.invalidate(ZosmfServiceV2.TokenType.LTPA, "ltpa");
            fail();
        } catch (ServiceNotAccessibleException e) {
            assertEquals("Could not get an access to z/OSMF service.", e.getMessage());
        }

        reset(restTemplate);
        when(restTemplate.exchange(anyString(), (HttpMethod) any(), (HttpEntity) any(), (Class<?>) any()))
            .thenThrow(new RestClientException("msg"));
        try {
            zosmfService.invalidate(ZosmfServiceV2.TokenType.LTPA, "ltpa");
            fail();
        } catch (AuthenticationServiceException e) {
            assertEquals("A failure occurred when authenticating.", e.getMessage());
            assertTrue(e.getCause() instanceof RestClientException);
        }
    }

    @Test
    public void testValidate() {
        mockZosmfService("zosmf", 1433);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-CSRF-ZOSMF-HEADER", "");
        headers.add(HttpHeaders.COOKIE, "LtpaToken2=ltpa");

        when(restTemplate.exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.POST,
            new HttpEntity<>(null, headers),
            String.class
        )).thenReturn(new ResponseEntity(HttpStatus.OK));

        zosmfService.validate(ZosmfServiceV2.TokenType.LTPA, "ltpa");;
    }

    @Test
    public void testValidateException() {
        mockZosmfService("zosmf", 1433);

        try {
            zosmfService.validate(ZosmfServiceV2.TokenType.LTPA, "token");
        } catch (NullPointerException ne) {
            // response is null
        }

        // response 401
        when(restTemplate.exchange(anyString(), (HttpMethod) any(), (HttpEntity) any(), (Class<?>) any()))
            .thenReturn(new ResponseEntity(HttpStatus.UNAUTHORIZED));
        try {
            zosmfService.validate(ZosmfServiceV2.TokenType.LTPA, "token");
        } catch (TokenNotValidException e) {
            assertEquals("Token is not valid.", e.getMessage());
        }

        // response 500
        when(restTemplate.exchange(anyString(), (HttpMethod) any(), (HttpEntity) any(), (Class<?>) any()))
            .thenReturn(new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR));
        try {
            zosmfService.validate(ZosmfServiceV2.TokenType.LTPA, "token");
        } catch (ServiceNotAccessibleException e) {
            assertEquals("Could not get an access to z/OSMF service.", e.getMessage());
        }
    }

    @Test
    public void testAuthenticate() {
        mockZosmfService("zosmf", 1433);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        requestHeaders.add("X-CSRF-ZOSMF-HEADER", "");

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.SET_COOKIE, "jwtToken=jt");
        responseHeaders.add(HttpHeaders.SET_COOKIE, "LtpaToken2=lt");
        ResponseEntity responseEntity = new ResponseEntity("{\"zosmf_saf_realm\":\"domain\"}", responseHeaders, HttpStatus.OK);

        when(restTemplate.exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.POST,
            new HttpEntity<>(null, requestHeaders),
            String.class
        )).thenReturn(responseEntity);

        ZosmfService.AuthenticationResponse response = zosmfService.authenticate(
            new UsernamePasswordAuthenticationToken("user", "pass")
        );
        assertNotNull(response);
        assertNotNull(response.getTokens());
        assertEquals(2, response.getTokens().size());
        assertEquals("lt", response.getTokens().get(ZosmfService.TokenType.LTPA));
        assertEquals("jt", response.getTokens().get(ZosmfService.TokenType.JWT));
        assertEquals("domain", response.getDomain());
    }

    @Test
    public void testAuthenticateToZosmfException() {
        mockZosmfService("zosmf", 1433);

        // unsupported runtime exception
        when(restTemplate.exchange(anyString(), (HttpMethod) any(), (HttpEntity) any(), (Class<?>) any()))
            .thenThrow(new IllegalArgumentException("msg"));
        try {
            zosmfService.validate(ZosmfServiceV2.TokenType.LTPA, "token");
        } catch (IllegalArgumentException e) {
            assertEquals("msg", e.getMessage());
        }

        // ResourceAccessException
        reset(restTemplate);
        when(restTemplate.exchange(anyString(), (HttpMethod) any(), (HttpEntity) any(), (Class<?>) any()))
            .thenThrow(new ResourceAccessException("msg"));
        try {
            zosmfService.validate(ZosmfServiceV2.TokenType.LTPA, "token");
        } catch (ServiceNotAccessibleException e) {
            assertEquals("Could not get an access to z/OSMF service.", e.getMessage());
        }

        // RestClientException
        reset(restTemplate);
        when(restTemplate.exchange(anyString(), (HttpMethod) any(), (HttpEntity) any(), (Class<?>) any()))
            .thenThrow(new RestClientException("msg"));
        try {
            zosmfService.validate(ZosmfServiceV2.TokenType.LTPA, "token");
        } catch (AuthenticationServiceException e) {
            assertEquals("A failure occurred when authenticating.", e.getMessage());
            assertTrue(e.getCause() instanceof RestClientException);
        }
    }

    @Test
    public void testMatchesVersion() {
        assertFalse(zosmfService.matchesVersion(Integer.MIN_VALUE));
        assertFalse(zosmfService.matchesVersion(25));
        assertTrue(zosmfService.matchesVersion(26));
        assertTrue(zosmfService.matchesVersion(Integer.MAX_VALUE));
    }

}
