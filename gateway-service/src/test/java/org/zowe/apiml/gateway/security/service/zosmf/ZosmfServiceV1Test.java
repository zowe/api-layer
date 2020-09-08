/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
//package org.zowe.apiml.gateway.security.service.zosmf;
//
//import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
//import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
//import org.zowe.apiml.security.common.token.TokenNotValidException;
//import org.zowe.apiml.gateway.security.service.ZosmfService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.netflix.appinfo.InstanceInfo;
//import com.netflix.discovery.DiscoveryClient;
//import com.netflix.discovery.shared.Application;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Spy;
//import org.mockito.junit.MockitoJUnitRunner;
//import org.springframework.http.*;
//import org.springframework.security.authentication.AuthenticationServiceException;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.web.client.RestClientException;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Collections;
//
//import static org.junit.Assert.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@RunWith(MockitoJUnitRunner.class)
//public class ZosmfServiceV1Test {
//
//    private static final String ZOSMF_ID = "zosmf";
//
//    @Mock
//    private AuthConfigurationProperties authConfigurationProperties;
//
//    @Mock
//    private DiscoveryClient discovery;
//
//    @Mock
//    private RestTemplate restTemplate;
//
//    @Spy
//    private ObjectMapper securityObjectMapper;
//
//    @InjectMocks
//    private ZosmfServiceV1 zosmfService;
//
//    @Before
//    public void setUp() {
//        when(authConfigurationProperties.validatedZosmfServiceId()).thenReturn(ZOSMF_ID);
//    }
//
//    private void mockZosmfService(String hostname, int port) {
//        InstanceInfo instanceInfo = mock(InstanceInfo.class);
//        when(instanceInfo.getHostName()).thenReturn(hostname);
//        when(instanceInfo.getPort()).thenReturn(port);
//        Application application = mock(Application.class);
//        when(application.getInstances()).thenReturn(Collections.singletonList(instanceInfo));
//        when(discovery.getApplication(ZOSMF_ID)).thenReturn(application);
//    }
//
//    @Test
//    public void testAuthenticate() {
//        mockZosmfService("zosmf", 1433);
//
//        HttpHeaders requestHeaders = new HttpHeaders();
//        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
//        requestHeaders.add("X-CSRF-ZOSMF-HEADER", "");
//
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.add(HttpHeaders.SET_COOKIE, "LtpaToken2=lt");
//        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"zosmf_saf_realm\":\"domain\"}", responseHeaders, HttpStatus.OK);
//
//        when(restTemplate.exchange(
//            "http://zosmf:1433/zosmf/info",
//            HttpMethod.GET,
//            new HttpEntity<>(null, requestHeaders),
//            String.class
//        )).thenReturn(responseEntity);
//
//        ZosmfService.AuthenticationResponse response = zosmfService.authenticate(
//            new UsernamePasswordAuthenticationToken("user", "pass")
//        );
//        assertNotNull(response);
//        assertNotNull(response.getTokens());
//        assertEquals(1, response.getTokens().size());
//        assertEquals("lt", response.getTokens().get(ZosmfService.TokenType.LTPA));
//        //provided via Facade
//        assertNull(response.getDomain());
//    }
//
//    @Test(expected = AuthenticationServiceException.class)
//    public void testAuthenticateException() {
//        mockZosmfService("zosmf", 1433);
//        when(restTemplate.exchange(
//            anyString(),
//            (HttpMethod) any(),
//            (HttpEntity<?>) any(),
//            (Class<?>) any()
//        )).thenThrow(new RestClientException("any exception"));
//
//        zosmfService.authenticate(new UsernamePasswordAuthenticationToken("user", "pass"));
//    }
//
//    @Test
//    public void testValidateValid() {
//        mockZosmfService("zosmf", 1433);
//
//        HttpHeaders requestHeaders = new HttpHeaders();
//        requestHeaders.add(HttpHeaders.COOKIE, "LtpaToken2=validToken");
//        requestHeaders.add("X-CSRF-ZOSMF-HEADER", "");
//
//        when(restTemplate.exchange(
//            "http://zosmf:1433/zosmf/info",
//            HttpMethod.GET,
//            new HttpEntity<>(null, requestHeaders),
//            String.class
//        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));
//
//        zosmfService.validate(ZosmfService.TokenType.LTPA, "validToken");
//    }
//
//    @Test
//    public void testValidateInvalid() {
//        mockZosmfService("zosmf", 1433);
//
//        HttpHeaders requestHeaders = new HttpHeaders();
//        requestHeaders.add(HttpHeaders.COOKIE, "LtpaToken2=invalidToken");
//        requestHeaders.add("X-CSRF-ZOSMF-HEADER", "");
//
//        when(restTemplate.exchange(
//            "http://zosmf:1433/zosmf/info",
//            HttpMethod.GET,
//            new HttpEntity<>(null, requestHeaders),
//            String.class
//        )).thenReturn(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
//
//        try {
//            zosmfService.validate(ZosmfService.TokenType.LTPA, "invalidToken");
//            fail();
//        } catch (TokenNotValidException e) {
//            // access denied - not valid
//        }
//    }
//
//    @Test
//    public void testValidateInternalError() {
//        mockZosmfService("zosmf", 1433);
//
//        when(restTemplate.exchange(
//            eq("http://zosmf:1433/zosmf/info"),
//            eq(HttpMethod.GET),
//            (HttpEntity<?>) any(),
//            (Class<?>) any()
//        )).thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
//
//        try {
//            zosmfService.validate(ZosmfService.TokenType.LTPA, "invalidToken");
//            fail();
//        } catch (ServiceNotAccessibleException e) {
//            // access denied - not valid
//        }
//    }
//
//    @Test
//    public void testValidateRuntimeException() {
//        mockZosmfService("zosmf", 1433);
//
//        when(restTemplate.exchange(
//            eq("http://zosmf:1433/zosmf/info"),
//            eq(HttpMethod.GET),
//            (HttpEntity<?>) any(),
//            (Class<?>) any()
//        )).thenThrow(new IllegalArgumentException("exception"));
//
//        try {
//            zosmfService.validate(ZosmfService.TokenType.LTPA, "invalidToken");
//            fail();
//        } catch (IllegalArgumentException e) {
//            // access denied - not valid
//        }
//    }
//
//    @Test
//    public void testInvalidate() {
//        ZosmfServiceV1 service = new ZosmfServiceV1(null, null, null, null);
//        service.invalidate(null, null);
//        assertNull(service.restTemplateWithoutKeystore);
//    }
//
//    @Test
//    public void testIsSupported() {
//        assertTrue(zosmfService.isSupported(Integer.MIN_VALUE));
//        assertTrue(zosmfService.isSupported(25));
//        assertTrue(zosmfService.isSupported(26));
//        assertTrue(zosmfService.isSupported(Integer.MAX_VALUE));
//    }
//
//}
