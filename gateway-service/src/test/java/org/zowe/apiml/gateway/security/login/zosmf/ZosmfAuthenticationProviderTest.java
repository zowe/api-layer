/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.login.zosmf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.security.common.config.AuthConfigurationProperties.JWT_AUTOCONFIGURATION_MODE.*;

class ZosmfAuthenticationProviderTest {

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
    private static final String INVALID_RESPONSE = "{\"saf_realm\": \"" + DOMAIN + "\"}";
    private UsernamePasswordAuthenticationToken usernamePasswordAuthentication;
    private AuthConfigurationProperties authConfigurationProperties;
    private DiscoveryClient discovery;
    private RestTemplate restTemplate;
    private InstanceInfo zosmfInstance;
    private AuthenticationService authenticationService;
    private final ObjectMapper securityObjectMapper = new ObjectMapper();
    protected static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";

    private ZosmfService.AuthenticationResponse getResponse(boolean valid) {
        if (valid) return new ZosmfService.AuthenticationResponse(RESPONSE, null);
        return new ZosmfService.AuthenticationResponse(INVALID_RESPONSE, null);
    }

    @BeforeEach
    void setUp() {
        usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD);
        authConfigurationProperties = new AuthConfigurationProperties();
        authConfigurationProperties.getZosmf().setJwtAutoconfiguration(AUTO);
        discovery = mock(DiscoveryClient.class);
        authenticationService = mock(AuthenticationService.class);
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

    private Application createApplication(InstanceInfo... instanceInfos) {
        Application out = mock(Application.class);
        when(out.getInstances()).thenReturn(Arrays.asList(instanceInfos));
        return out;
    }

    private ZosmfService createZosmfService() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        ZosmfService zosmfService = new ZosmfService(authConfigurationProperties,
            discovery,
            restTemplate,
            securityObjectMapper,
            applicationContext,
            new ArrayList<>());
        ReflectionTestUtils.setField(zosmfService, "meAsProxy", zosmfService);
        ZosmfService output = spy(zosmfService);
        when(applicationContext.getBean(ZosmfService.class)).thenReturn(output);
        return output;
    }

    @Test
    void loginWithExistingUser() {
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE1);
        headers.add(HttpHeaders.SET_COOKIE, COOKIE2);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(getResponse(true), headers, HttpStatus.OK));

        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        mockZosmfRealmRestCallResponse();
        mockZosmfRealmRestCallResponse();
        Authentication tokenAuthentication
            = zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        assertTrue(tokenAuthentication.isAuthenticated());
        assertEquals(USERNAME, tokenAuthentication.getPrincipal());
    }

    @Test
    void loginWithBadUser() {
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(getResponse(true), headers, HttpStatus.OK));

        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        mockZosmfRealmRestCallResponse();
        Exception exception = assertThrows(BadCredentialsException.class,
            () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
            "Expected exception is not BadCredentialsException");
        assertEquals("Username or password are invalid.", exception.getMessage());
    }

    @Test
    void noZosmfInstance() {
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication();
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        Exception exception = assertThrows(ServiceNotAccessibleException.class,
            () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
            "Expected exception is not ServiceNotAccessibleException");
        assertEquals("z/OSMF instance not found or incorrectly configured.", exception.getMessage());
    }

    @Test
    void noZosmfServiceId() {
        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        Exception exception = assertThrows(AuthenticationServiceException.class,
            () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
            "Expected exception is not AuthenticationServiceException");
        assertEquals("The parameter 'apiml.security.auth.zosmf.serviceId' is not configured.", exception.getMessage());
    }

    @Test
    void notValidZosmfResponse() {
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE1);
        headers.add(HttpHeaders.SET_COOKIE, COOKIE2);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(new ZosmfService.ZosmfInfo(), headers, HttpStatus.OK));

        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        Exception exception = assertThrows(AuthenticationServiceException.class,
            () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
            "Expected exception is not AuthenticationServiceException");
        assertEquals("z/OSMF domain cannot be read.", exception.getMessage());
    }

    @Test
    void noDomainInResponse() throws IOException {
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        ZosmfService.ZosmfInfo zosmfInfoNoDomain =
            securityObjectMapper.reader().forType(ZosmfService.ZosmfInfo.class).readValue(INVALID_RESPONSE);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE1);
        headers.add(HttpHeaders.SET_COOKIE, COOKIE2);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(zosmfInfoNoDomain, headers, HttpStatus.OK));

        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        Exception exception = assertThrows(AuthenticationServiceException.class,
            () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
            "Expected exception is not AuthenticationServiceException");
        assertEquals("z/OSMF domain cannot be read.", exception.getMessage());
    }

    @Test
    void invalidCookieInResponse() {
        String invalidCookie = "LtpaToken=test";

        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, invalidCookie);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(getResponse(true), headers, HttpStatus.OK));
        mockZosmfRealmRestCallResponse();
        ZosmfService zosmfService = createZosmfService();
        doReturn(false).when(zosmfService).loginEndpointExists();

        mockZosmfRealmRestCallResponse();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);
        Exception exception = assertThrows(BadCredentialsException.class,
            () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
            "Expected exception is not BadCredentialsException");
        assertEquals("Username or password are invalid.", exception.getMessage());
    }

    private void mockZosmfRealmRestCallResponse() {
        final HttpHeaders zosmfheaders = new HttpHeaders();
        zosmfheaders.add(ZOSMF_CSRF_HEADER, "");
        ZosmfService.ZosmfInfo info = new ZosmfService.ZosmfInfo();
        info.setSafRealm("realm");
        when(restTemplate.exchange(eq("http://localhost:0/zosmf/info"),
            Mockito.eq(HttpMethod.GET),
            eq(new HttpEntity<>(null, zosmfheaders)),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(info, zosmfheaders, HttpStatus.OK));
    }

    @Test
    void cookieWithSemicolon() {
        String cookie = "LtpaToken2=test;";

        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(getResponse(true), headers, HttpStatus.OK));

        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        mockZosmfRealmRestCallResponse();
        Authentication tokenAuthentication = zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        assertTrue(tokenAuthentication.isAuthenticated());
        assertEquals(USERNAME, tokenAuthentication.getPrincipal());
    }

    @Test
    void shouldThrowNewExceptionIfRestClientException() {
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenThrow(RestClientException.class);
        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        Exception exception = assertThrows(AuthenticationServiceException.class,
            () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
            "Expected exception is not AuthenticationServiceException");
        assertEquals("A failure occurred when authenticating.", exception.getMessage());
    }

    @Test
    void shouldThrowNewExceptionIfResourceAccessException() {
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);
        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(),
            Mockito.<Class<Object>>any()))
            .thenThrow(ResourceAccessException.class);
        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        Exception exception = assertThrows(ServiceNotAccessibleException.class,
            () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
            "Expected exception is not ServiceNotAccessibleException");
        assertEquals("Could not get an access to z/OSMF service.", exception.getMessage());
    }

    @Test
    void shouldReturnTrueWhenSupportMethodIsCalledWithCorrectClass() {
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);
        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        boolean supports = zosmfAuthenticationProvider.supports(usernamePasswordAuthentication.getClass());
        assertTrue(supports);
    }

    @Test
    void testSupports() {
        ZosmfAuthenticationProvider mock = new ZosmfAuthenticationProvider(null, null, null);

        assertTrue(mock.supports(UsernamePasswordAuthenticationToken.class));
        assertFalse(mock.supports(Object.class));
        assertFalse(mock.supports(AbstractAuthenticationToken.class));
        assertFalse(mock.supports(JaasAuthenticationToken.class));
        assertFalse(mock.supports(null));
    }

    @Test
    void testAuthenticateJwt() {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        ZosmfService zosmfService = mock(ZosmfService.class);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider = new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);
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

    @Test
    void testJwt_givenZosmfJwt_whenItIsIgnoring_thenCreateZoweJwt() {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        ZosmfService zosmfService = mock(ZosmfService.class);
        ZosmfAuthenticationProvider zosmfAuthenticationProvider = new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        EnumMap<ZosmfService.TokenType, String> tokens = new EnumMap<>(ZosmfService.TokenType.class);
        tokens.put(ZosmfService.TokenType.LTPA, "ltpaToken");
        when(zosmfService.authenticate(any())).thenReturn(new ZosmfService.AuthenticationResponse("domain", tokens));

        zosmfAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("userId", "password"));

        verify(authenticationService, times(1)).createJwtToken("userId", "domain", "ltpaToken");
    }

    @Nested
    class givenOverride {

        private ZosmfAuthenticationProvider underTest;
        private AuthenticationService authenticationService;
        private EnumMap<ZosmfService.TokenType, String> tokens;
        private final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken("userId", "password");

        @BeforeEach
        void setUp() {
            authenticationService = mock(AuthenticationService.class);
            when(authenticationService.createJwtToken(any(), any(), any())).thenReturn("ltpaToken");
            ZosmfService zosmfService = mock(ZosmfService.class);
            underTest = new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

            tokens = new EnumMap<>(ZosmfService.TokenType.class);


            when(zosmfService.authenticate(any())).thenReturn(new ZosmfService.AuthenticationResponse("domain", tokens));
        }

        @Test
        void willChooseJwtWhenPresent() {
            tokens.put(ZosmfService.TokenType.LTPA, "ltpaToken");
            tokens.put(ZosmfService.TokenType.JWT, "jwtToken");

            underTest.authenticate(usernamePasswordAuthenticationToken);
            verify(authenticationService, atLeastOnce()).createTokenAuthentication("userId", "jwtToken");
        }

        @Test
        void willChooseLtpaWhenOnlyLtpa() {
            tokens.put(ZosmfService.TokenType.LTPA, "ltpaToken");

            underTest.authenticate(usernamePasswordAuthenticationToken);
            verify(authenticationService, atLeastOnce()).createTokenAuthentication("userId", "ltpaToken");
        }

        @Test
        void willChooseLtpaWhenOverride() {
            authConfigurationProperties.getZosmf().setJwtAutoconfiguration(LTPA);
            tokens.put(ZosmfService.TokenType.LTPA, "ltpaToken");
            tokens.put(ZosmfService.TokenType.JWT, "jwtToken");
            underTest.authenticate(usernamePasswordAuthenticationToken);
            verify(authenticationService, atLeastOnce()).createTokenAuthentication("userId", "ltpaToken");
        }

        @Test
        void willChooseJwtWhenOverride() {
            authConfigurationProperties.getZosmf().setJwtAutoconfiguration(JWT);
            tokens.put(ZosmfService.TokenType.LTPA, "ltpaToken");
            tokens.put(ZosmfService.TokenType.JWT, "jwtToken");
            underTest.authenticate(usernamePasswordAuthenticationToken);
            verify(authenticationService, atLeastOnce()).createTokenAuthentication("userId", "jwtToken");
        }

        @Test
        void willThrowWhenOverrideAndNoTokens() {
            authConfigurationProperties.getZosmf().setJwtAutoconfiguration(JWT);
            tokens.put(ZosmfService.TokenType.LTPA, "ltpaToken");
            assertThrows(BadCredentialsException.class, () -> underTest.authenticate(usernamePasswordAuthenticationToken));
        }

        @Test
        void willThrowWhenOverrideAndNoTokens2() {
            authConfigurationProperties.getZosmf().setJwtAutoconfiguration(LTPA);
            tokens.put(ZosmfService.TokenType.JWT, "jwtToken");
            assertThrows(BadCredentialsException.class, () -> underTest.authenticate(usernamePasswordAuthenticationToken));
        }
    }
}
