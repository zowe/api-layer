/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.login.zosmf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.InvalidTokenTypeException;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.security.common.config.AuthConfigurationProperties.JWT_AUTOCONFIGURATION_MODE.*;

@ExtendWith(MockitoExtension.class)
class ZosmfAuthenticationProviderTest {

    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";
    private static final String HOST = "localhost";
    private static final int PORT = 0;
    private static final String ZOSMF = "zosmf";
    private static final String COOKIE1 = "Cookie1=testCookie1";
    private static final String COOKIE2 = "LtpaToken2=test";
    private static final String DOMAIN = "realm";
    private static final String INVALID_RESPONSE = "{\"saf_realm\": \"" + DOMAIN + "\"}";

    @Mock
    private DiscoveryClient discovery;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TokenCreationService tokenCreationService;


    private UsernamePasswordAuthenticationToken usernamePasswordAuthentication;
    private AuthConfigurationProperties authConfigurationProperties;
    private InstanceInfo zosmfInstance;
    private final ObjectMapper securityObjectMapper = new ObjectMapper();


    private ZosmfService.ZosmfInfo getZosmfResponse() {
        ZosmfService.ZosmfInfo info = new ZosmfService.ZosmfInfo();
        info.setSafRealm("realm");

        return info;
    }

    private InstanceInfo createInstanceInfo(String host, int port) {
        InstanceInfo out = mock(InstanceInfo.class);
        lenient().when(out.getHostName()).thenReturn(host);
        lenient().when(out.getPort()).thenReturn(port);
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
            authenticationService,
            tokenCreationService,
            new ArrayList<>());
        ReflectionTestUtils.setField(zosmfService, "meAsProxy", zosmfService);

        return spy(zosmfService);
    }

    private void mockZosmfAuthenticationRestCallResponse() {
        when(restTemplate.exchange(eq("http://localhost:0/zosmf/services/authenticate"),
            Mockito.eq(HttpMethod.POST),
            any(),
            Mockito.<Class<Object>>any()))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
    }

    @BeforeEach
    void setUp() {
        usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD);
        authConfigurationProperties = new AuthConfigurationProperties();
        authConfigurationProperties.getZosmf().setJwtAutoconfiguration(AUTO);
        zosmfInstance = createInstanceInfo(HOST, PORT);

        lenient().doAnswer((Answer<TokenAuthentication>) invocation -> TokenAuthentication.createAuthenticated(invocation.getArgument(0), invocation.getArgument(1))).when(authenticationService).createTokenAuthentication(anyString(), anyString());
        lenient().when(authenticationService.createJwtToken(anyString(), anyString(), anyString())).thenReturn("someJwtToken");
    }

    @Test
    void loginWithExistingUser() {
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        mockZosmfAuthenticationRestCallResponse();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE1);
        headers.add(HttpHeaders.SET_COOKIE, COOKIE2);
        when(restTemplate.exchange(eq("http://localhost:0/zosmf/info"),
            eq(HttpMethod.GET),
            any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(getZosmfResponse(), headers, HttpStatus.OK));

        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

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

        mockZosmfAuthenticationRestCallResponse();
        when(restTemplate.exchange(eq("http://localhost:0/zosmf/info"),
            eq(HttpMethod.GET),
            any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(getZosmfResponse(), new HttpHeaders(), HttpStatus.OK));

        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        Exception exception = assertThrows(BadCredentialsException.class,
            () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
            "Expected exception is not BadCredentialsException");
        assertEquals("Invalid Credentials", exception.getMessage());
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

        mockZosmfAuthenticationRestCallResponse();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE1);
        headers.add(HttpHeaders.SET_COOKIE, COOKIE2);
        when(restTemplate.exchange(eq("http://localhost:0/zosmf/info"),
            eq(HttpMethod.GET),
            any(),
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

        mockZosmfAuthenticationRestCallResponse();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, COOKIE1);
        headers.add(HttpHeaders.SET_COOKIE, COOKIE2);
        when(restTemplate.exchange(eq("http://localhost:0/zosmf/info"),
            eq(HttpMethod.GET),
            any(),
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

        mockZosmfAuthenticationRestCallResponse();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, invalidCookie);
        when(restTemplate.exchange(eq("http://localhost:0/zosmf/info"),
            eq(HttpMethod.GET),
            any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(getZosmfResponse(), headers, HttpStatus.OK));
        ZosmfService zosmfService = createZosmfService();

        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);
        Exception exception = assertThrows(BadCredentialsException.class,
            () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
            "Expected exception is not BadCredentialsException");
        assertEquals("Invalid Credentials", exception.getMessage());
    }

    @Test
    void cookieWithSemicolon() {
        String cookie = "LtpaToken2=test;";

        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        mockZosmfAuthenticationRestCallResponse();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie);
        when(restTemplate.exchange(eq("http://localhost:0/zosmf/info"),
            eq(HttpMethod.GET),
            any(),
            Mockito.<Class<Object>>any()))
            .thenReturn(new ResponseEntity<>(getZosmfResponse(), headers, HttpStatus.OK));

        ZosmfService zosmfService = createZosmfService();
        ZosmfAuthenticationProvider zosmfAuthenticationProvider =
            new ZosmfAuthenticationProvider(authenticationService, zosmfService, authConfigurationProperties);

        Authentication tokenAuthentication = zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        assertTrue(tokenAuthentication.isAuthenticated());
        assertEquals(USERNAME, tokenAuthentication.getPrincipal());
    }

    @Test
    void shouldThrowNewExceptionIfRestClientException() {
        authConfigurationProperties.getZosmf().setServiceId(ZOSMF);

        final Application application = createApplication(zosmfInstance);
        when(discovery.getApplication(ZOSMF)).thenReturn(application);

        mockZosmfAuthenticationRestCallResponse();
        when(restTemplate.exchange(eq("http://localhost:0/zosmf/info"),
            eq(HttpMethod.GET),
            any(),
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

        mockZosmfAuthenticationRestCallResponse();
        when(restTemplate.exchange(eq("http://localhost:0/zosmf/info"),
            eq(HttpMethod.GET),
            any(),
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

        @Mock
        private AuthenticationService authenticationService;

        private ZosmfAuthenticationProvider underTest;
        private EnumMap<ZosmfService.TokenType, String> tokens;
        private final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken("userId", "password");

        @BeforeEach
        void setUp() {
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
            when(authenticationService.createJwtToken(any(), any(), any())).thenReturn("ltpaToken");
            tokens.put(ZosmfService.TokenType.LTPA, "ltpaToken");

            underTest.authenticate(usernamePasswordAuthenticationToken);
            verify(authenticationService, atLeastOnce()).createTokenAuthentication("userId", "ltpaToken");
        }

        @Test
        void willChooseLtpaWhenOverride() {
            when(authenticationService.createJwtToken(any(), any(), any())).thenReturn("ltpaToken");

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
        void willThrowWhenOverrideAndWrongTokenLtpa() {
            authConfigurationProperties.getZosmf().setJwtAutoconfiguration(JWT);
            tokens.put(ZosmfService.TokenType.LTPA, "ltpaToken");
            assertThrows(InvalidTokenTypeException.class, () -> underTest.authenticate(usernamePasswordAuthenticationToken));
        }

        @Test
        void willThrowWhenOverrideAndWrongTokenJwt() {
            authConfigurationProperties.getZosmf().setJwtAutoconfiguration(LTPA);
            tokens.put(ZosmfService.TokenType.JWT, "jwtToken");
            assertThrows(InvalidTokenTypeException.class, () -> underTest.authenticate(usernamePasswordAuthenticationToken));
        }

        @Test
        void willThrowBadCredentialsWhenNoTokenPresentExpectingLtpa() {
            authConfigurationProperties.getZosmf().setJwtAutoconfiguration(LTPA);
            assertThrows(BadCredentialsException.class, () -> underTest.authenticate(usernamePasswordAuthentication));
        }

        @Test
        void willThrowBadCredentialsWhenNoTokenPresentExpectingJwt() {
            authConfigurationProperties.getZosmf().setJwtAutoconfiguration(JWT);
            assertThrows(BadCredentialsException.class, () -> underTest.authenticate(usernamePasswordAuthentication));
        }
    }
}
