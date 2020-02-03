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

import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import com.ca.apiml.security.common.error.ServiceNotAccessibleException;
import com.ca.apiml.security.common.token.TokenAuthentication;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.ca.mfaas.gateway.security.service.ZosmfService;
import com.ca.mfaas.gateway.security.service.zosmf.ZosmfServiceV2;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ZosmfAuthenticationProviderTest {

    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";
    private static final String SERVICE_ID = "service";
    private static final String HOST = "localhost";
    private static final int PORT = 0;
    private static final String ZOSMF = "zosmf";
    private static final String COOKIE1 = "Cookie1=testCookie1";
    private static final String COOKIE2 = "LtpaToken2=test";
    private static final String DOMAIN = "realm";
    private static final String RESPONSE = "{\"zosmf_saf_realm\": \"" + DOMAIN + "\"}";

    private UsernamePasswordAuthenticationToken usernamePasswordAuthentication;
    private AuthConfigurationProperties authConfigurationProperties;
    private DiscoveryClient discovery;
    private ObjectMapper mapper;
    private RestTemplate restTemplate;
    private InstanceInfo zosmfInstance;
    private AuthenticationService authenticationService;
    private ObjectMapper securityObjectMapper = new ObjectMapper();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD);
        authConfigurationProperties = new AuthConfigurationProperties();
        discovery = mock(DiscoveryClient.class);
        authenticationService = mock(AuthenticationService.class);
        mapper = new ObjectMapper();
        restTemplate = mock(RestTemplate.class);
        zosmfInstance = createInstanceInfo(SERVICE_ID, HOST, PORT);

        doAnswer((Answer<TokenAuthentication>) invocation -> TokenAuthentication.createAuthenticated(invocation.getArgument(0), invocation.getArgument(1))).when(authenticationService).createTokenAuthentication(anyString(), anyString());
        when(authenticationService.createJwtToken(anyString(), anyString(), anyString())).thenReturn("someJwtToken");
    }

    private InstanceInfo createInstanceInfo(String serviceId, String host, int port) {
        InstanceInfo out = mock(InstanceInfo.class);
        when(out.getAppName()).thenReturn(serviceId);
        when(out.getHostName()).thenReturn(host);
        when(out.getPort()).thenReturn(port);
        return out;
    }

    private Application createApplication(InstanceInfo...instanceInfos) {
        Application out = mock(Application.class);
        when(out.getInstances()).thenReturn(Arrays.asList(instanceInfos));
        return out;
    }

    @Test
    public void loginWithExistingUser() {
        authConfigurationProperties.setZosmfServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE1);
        headers.add(HttpHeaders.SET_COOKIE, COOKIE2);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.POST),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(RESPONSE, headers, HttpStatus.OK));

        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        Authentication tokenAuthentication
            = zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        assertTrue(tokenAuthentication.isAuthenticated());
        assertEquals(USERNAME, tokenAuthentication.getPrincipal());
    }

    @Test
    public void loginWithBadUser() {
        authConfigurationProperties.setZosmfServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.POST),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(RESPONSE, headers, HttpStatus.OK));

        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Username or password are invalid.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void noZosmfInstance() {
        authConfigurationProperties.setZosmfServiceId(ZOSMF);

        final Application application = createApplication();
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        exception.expect(ServiceNotAccessibleException.class);
        exception.expectMessage("z/OSMF instance not found or incorrectly configured.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void noZosmfServiceId() {
        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        exception.expect(AuthenticationServiceException.class);
        exception.expectMessage("The parameter 'zosmfServiceId' is not configured.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void notValidZosmfResponse() {
        authConfigurationProperties.setZosmfServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE1);
        headers.add(HttpHeaders.SET_COOKIE, COOKIE2);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.POST),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>("", headers, HttpStatus.OK));

        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        exception.expect(AuthenticationServiceException.class);
        exception.expectMessage("z/OSMF domain cannot be read.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void noDomainInResponse() {
        String invalidResponse = "{\"saf_realm\": \"" + DOMAIN + "\"}";

        authConfigurationProperties.setZosmfServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE1);
        headers.add(HttpHeaders.SET_COOKIE, COOKIE2);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.POST),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(invalidResponse, headers, HttpStatus.OK));

        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        exception.expect(AuthenticationServiceException.class);
        exception.expectMessage("z/OSMF domain cannot be read.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void invalidCookieInResponse() {
        String invalidCookie = "LtpaToken=test";

        authConfigurationProperties.setZosmfServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, invalidCookie);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.POST),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(RESPONSE, headers, HttpStatus.OK));

        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Username or password are invalid.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);
    }

    @Test
    public void cookieWithSemicolon() {
        String cookie = "LtpaToken2=test;";

        authConfigurationProperties.setZosmfServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.POST),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(RESPONSE, headers, HttpStatus.OK));

        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        Authentication tokenAuthentication
            = zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        assertTrue(tokenAuthentication.isAuthenticated());
        assertEquals(USERNAME, tokenAuthentication.getPrincipal());
    }

    @Test
    public void shouldThrowNewExceptionIfRestClientException() {
        authConfigurationProperties.setZosmfServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.POST),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenThrow(RestClientException.class);
        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        exception.expect(AuthenticationServiceException.class);
        exception.expectMessage("A failure occurred when authenticating.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

    }

    @Test
    public void shouldThrowNewExceptionIfResourceAccessException() {
        authConfigurationProperties.setZosmfServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.POST),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenThrow(ResourceAccessException.class);
        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        exception.expect(ServiceNotAccessibleException.class);
        exception.expectMessage("Could not get an access to z/OSMF service.");

        zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

    }

    @Test
    public void shouldReturnTrueWhenSupportMethodIsCalledWithCorrectClass() {
        authConfigurationProperties.setZosmfServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);
        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService);

        boolean supports = zosmfAuthenticationProvider.supports(usernamePasswordAuthentication.getClass());
        assertTrue(supports);
    }

    @Test
    public void testSupports() {
        ZosmfAuthenticationProvider mock = new ZosmfAuthenticationProvider(null, null);

        assertTrue(mock.supports(UsernamePasswordAuthenticationToken.class));
        assertFalse(mock.supports(Object.class));
        assertFalse(mock.supports(AbstractAuthenticationToken.class));
        assertFalse(mock.supports(JaasAuthenticationToken.class));
        assertFalse(mock.supports(null));
    }

    @Test
    public void testAuthenticateJwt() {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        ZosmfService zosmfService = mock(ZosmfService.class);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider = new ZosmfAuthenticationProvider(authenticationService, zosmfService);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("user1");
        TokenAuthentication authentication2 = mock(TokenAuthentication.class);

        when(zosmfService.authenticate(authentication)).thenReturn(new ZosmfService.AuthenticationResponse(
            "domain1",
            Collections.singletonMap(ZosmfService.TokenType.JWT, "jwtToken1")
        ));
        when(authenticationService.createTokenAuthentication("user1", "jwtToken1")).thenReturn(authentication2);

        assertSame(authentication2, zosmfAuthenticationProvider.authenticate(authentication));
    }

}
