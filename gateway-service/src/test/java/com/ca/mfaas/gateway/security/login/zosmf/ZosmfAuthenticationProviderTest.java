/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.gateway.security.login.zosmf;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.error.ServiceNotAccessibleException;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZosmfAuthenticationProviderTest {
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";
    private static final String SERVICE_ID = "service";
    private static final String HOST = "localhost";
    private static final int PORT = 0;
    private static final String ZOSMF = "zosmf";
    private static final String COOKIE = "LtpaToken2=test";
    private static final String DOMAIN = "realm";
    private static final String RESPONSE = "{\"zosmf_saf_realm\": \"" + DOMAIN + "\"}";

    private UsernamePasswordAuthenticationToken usernamePasswordAuthentication;
    private SecurityConfigurationProperties securityConfigurationProperties;
    private DiscoveryClient discovery;
    private ObjectMapper mapper;
    private RestTemplate restTemplate;
    private ServiceInstance zosmfInstance;
    private AuthenticationService authenticationService;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD);
        securityConfigurationProperties = new SecurityConfigurationProperties();
        discovery = mock(DiscoveryClient.class);
        authenticationService = mock(AuthenticationService.class);
        mapper = new ObjectMapper();
        restTemplate = mock(RestTemplate.class);
        zosmfInstance = new DefaultServiceInstance(SERVICE_ID, HOST, PORT, false);
    }

    @Test
    public void loginWithExistingUser() {
        securityConfigurationProperties.setZosmfServiceId(ZOSMF);

        List<ServiceInstance> zosmfInstances = Collections.singletonList(zosmfInstance);
        when(discovery.getInstances(ZOSMF)).thenReturn(zosmfInstances);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(RESPONSE, headers, HttpStatus.OK));

        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        Authentication tokenAuthentication
            = zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        assertTrue(tokenAuthentication.isAuthenticated());
        assertEquals(USERNAME, tokenAuthentication.getPrincipal());
    }
//
    @Test
    public void loginWithBadUser() {
        securityConfigurationProperties.setZosmfServiceId(ZOSMF);

        List<ServiceInstance> zosmfInstances = Collections.singletonList(zosmfInstance);
        when(discovery.getInstances(ZOSMF)).thenReturn(zosmfInstances);

        HttpHeaders headers = new HttpHeaders();
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(RESPONSE, headers, HttpStatus.OK));

        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Username or password are invalid.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void noZosmfInstance() {
        securityConfigurationProperties.setZosmfServiceId(ZOSMF);
        List<ServiceInstance> zosmfInstances = Collections.singletonList(null);
        when(discovery.getInstances(ZOSMF)).thenReturn(zosmfInstances);

        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        exception.expect(ServiceNotAccessibleException.class);
        exception.expectMessage("z/OSMF instance not found or incorrectly configured.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void noZosmfServiceId() {
        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        exception.expect(AuthenticationServiceException.class);
        exception.expectMessage("The parameter 'zosmfServiceId' is not configured.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void notValidZosmfResponse() {
        securityConfigurationProperties.setZosmfServiceId(ZOSMF);

        List<ServiceInstance> zosmfInstances = Collections.singletonList(zosmfInstance);
        when(discovery.getInstances(ZOSMF)).thenReturn(zosmfInstances);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>("", headers, HttpStatus.OK));

        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        exception.expect(AuthenticationServiceException.class);
        exception.expectMessage("z/OSMF domain cannot be read.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void noDomainInResponse() {
        String invalidResponse = "{\"saf_realm\": \"" + DOMAIN + "\"}";

        securityConfigurationProperties.setZosmfServiceId(ZOSMF);

        List<ServiceInstance> zosmfInstances = Collections.singletonList(zosmfInstance);
        when(discovery.getInstances(ZOSMF)).thenReturn(zosmfInstances);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(invalidResponse, headers, HttpStatus.OK));

        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        exception.expect(AuthenticationServiceException.class);
        exception.expectMessage("z/OSMF domain cannot be read.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void invalidCookieInResponse() {
        String invalidCookie = "LtpaToken=test";

        securityConfigurationProperties.setZosmfServiceId(ZOSMF);

        List<ServiceInstance> zosmfInstances = Collections.singletonList(zosmfInstance);
        when(discovery.getInstances(ZOSMF)).thenReturn(zosmfInstances);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, invalidCookie);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(RESPONSE, headers, HttpStatus.OK));

        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Username or password are invalid.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void cookieWithSemicolon() {
        String cookie = "LtpaToken2=test;";

        securityConfigurationProperties.setZosmfServiceId(ZOSMF);

        List<ServiceInstance> zosmfInstances = Collections.singletonList(zosmfInstance);
        when(discovery.getInstances(ZOSMF)).thenReturn(zosmfInstances);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(RESPONSE, headers, HttpStatus.OK));

        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        Authentication tokenAuthentication
            = zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        assertTrue(tokenAuthentication.isAuthenticated());
        assertEquals(USERNAME, tokenAuthentication.getPrincipal());
    }

    @Test
    public void shouldThrowNewExceptionIfRestClientException() {
        securityConfigurationProperties.setZosmfServiceId(ZOSMF);

        List<ServiceInstance> zosmfInstances = Collections.singletonList(zosmfInstance);
        when(discovery.getInstances(ZOSMF)).thenReturn(zosmfInstances);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenThrow(RestClientException.class);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        exception.expect(AuthenticationServiceException.class);
        exception.expectMessage("A failure occurred when authenticating.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

    }

    @Test
    public void shouldThrowNewExceptionIfResourceAccessException() {
        securityConfigurationProperties.setZosmfServiceId(ZOSMF);

        List<ServiceInstance> zosmfInstances = Collections.singletonList(zosmfInstance);
        when(discovery.getInstances(ZOSMF)).thenReturn(zosmfInstances);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenThrow(ResourceAccessException.class);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        exception.expect(ServiceNotAccessibleException.class);
        exception.expectMessage("Could not get an access to z/OSMF service.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

    }

    @Test
    public void shouldReturnTrueWhenSupportMethodIsCalledWithCorrectClass() {
        securityConfigurationProperties.setZosmfServiceId(ZOSMF);

        List<ServiceInstance> zosmfInstances = Collections.singletonList(zosmfInstance);
        when(discovery.getInstances(ZOSMF)).thenReturn(zosmfInstances);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider
            = new ZosmfAuthenticationProvider(securityConfigurationProperties, authenticationService, discovery, mapper, restTemplate);

        boolean supports = zosmfAuthenticationProvider.supports(usernamePasswordAuthentication.getClass());
        assertTrue(supports);

    }
}
