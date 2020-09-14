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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
   private ObjectMapper securityObjectMapper = new ObjectMapper();

    private ZosmfService.AuthenticationResponse getResponse(boolean valid) {
        if (valid) return new ZosmfService.AuthenticationResponse(RESPONSE, null);
        return new ZosmfService.AuthenticationResponse(INVALID_RESPONSE, null);
    }

   @BeforeEach
   void setUp() {
       usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD);
       authConfigurationProperties = new AuthConfigurationProperties();
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

   private Application createApplication(InstanceInfo...instanceInfos) {
       Application out = mock(Application.class);
       when(out.getInstances()).thenReturn(Arrays.asList(instanceInfos));
       return out;
   }

   private ZosmfService createZosmfService() {
       ZosmfService zosmfService = new ZosmfService(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
       ApplicationContext applicationContext = mock(ApplicationContext.class);
       ZosmfService output = spy(zosmfService);
       when(applicationContext.getBean(ZosmfService.class)).thenReturn(output);
       return output;
   }

   @Test
   void loginWithExistingUser() throws IOException {
       authConfigurationProperties.setZosmfServiceId(ZOSMF);

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
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);
       doReturn("realm").when(zosmfService).getZosmfRealm(Mockito.anyString());

       Authentication tokenAuthentication
           = zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

       assertTrue(tokenAuthentication.isAuthenticated());
       assertEquals(USERNAME, tokenAuthentication.getPrincipal());
   }

   @Test
   void loginWithBadUser() throws IOException {
       authConfigurationProperties.setZosmfServiceId(ZOSMF);

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
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);
       doReturn("realm").when(zosmfService).getZosmfRealm(Mockito.anyString());

       Exception exception = assertThrows(BadCredentialsException.class,
           () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
           "Expected exception is not BadCredentialsException");
       assertEquals("Username or password are invalid.", exception.getMessage());
   }

   @Test
   void noZosmfInstance() {
       authConfigurationProperties.setZosmfServiceId(ZOSMF);

       final Application application = createApplication();
       when(discovery.getApplication(ZOSMF)).thenReturn(application);

       ZosmfService zosmfService = createZosmfService();
       ZosmfAuthenticationProvider zosmfAuthenticationProvider =
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);

       Exception exception = assertThrows(ServiceNotAccessibleException.class,
           () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
           "Expected exception is not ServiceNotAccessibleException");
       assertEquals("z/OSMF instance not found or incorrectly configured.", exception.getMessage());
   }

   @Test
   void noZosmfServiceId() {
       ZosmfService zosmfService = createZosmfService();
       ZosmfAuthenticationProvider zosmfAuthenticationProvider =
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);

       Exception exception = assertThrows(AuthenticationServiceException.class,
           () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
           "Expected exception is not AuthenticationServiceException");
       assertEquals("The parameter 'zosmfServiceId' is not configured.", exception.getMessage());
   }

   @Test
   void notValidZosmfResponse() throws Exception {
       authConfigurationProperties.setZosmfServiceId(ZOSMF);

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
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);

       Exception exception = assertThrows(AuthenticationServiceException.class,
           () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
           "Expected exception is not AuthenticationServiceException");
       assertEquals("z/OSMF domain cannot be read.", exception.getMessage());
   }

   @Test
   void noDomainInResponse() throws IOException {
       authConfigurationProperties.setZosmfServiceId(ZOSMF);

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
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);

       Exception exception = assertThrows(AuthenticationServiceException.class,
           () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
           "Expected exception is not AuthenticationServiceException");
       assertEquals("z/OSMF domain cannot be read.", exception.getMessage());
   }

   @Test
   void invalidCookieInResponse() throws IOException {
       String invalidCookie = "LtpaToken=test";

       authConfigurationProperties.setZosmfServiceId(ZOSMF);

       final Application application = createApplication(zosmfInstance);
       when(discovery.getApplication(ZOSMF)).thenReturn(application);

       HttpHeaders headers = new HttpHeaders();
       headers.add(HttpHeaders.SET_COOKIE, invalidCookie);
       when(restTemplate.exchange(Mockito.anyString(),
           Mockito.eq(HttpMethod.GET),
           Mockito.any(),
           Mockito.<Class<Object>>any()))
           .thenReturn(new ResponseEntity<>(getResponse(true), headers, HttpStatus.OK));

       ZosmfService zosmfService = createZosmfService();
       doReturn(false).when(zosmfService).authenticationEndpointExists(HttpMethod.POST);
       doReturn("realm").when(zosmfService).getZosmfRealm(Mockito.anyString());
       ZosmfAuthenticationProvider zosmfAuthenticationProvider =
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);

       Exception exception = assertThrows(BadCredentialsException.class,
           () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
           "Expected exception is not BadCredentialsException");
       assertEquals("Username or password are invalid.", exception.getMessage());
   }

   @Test
   void cookieWithSemicolon() throws IOException {
       String cookie = "LtpaToken2=test;";

       authConfigurationProperties.setZosmfServiceId(ZOSMF);

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
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);
       doReturn("realm").when(zosmfService).getZosmfRealm(Mockito.anyString());

       Authentication tokenAuthentication = zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication);

       assertTrue(tokenAuthentication.isAuthenticated());
       assertEquals(USERNAME, tokenAuthentication.getPrincipal());
   }

   @Test
   void shouldThrowNewExceptionIfRestClientException() {
       authConfigurationProperties.setZosmfServiceId(ZOSMF);

       final Application application = createApplication(zosmfInstance);
       when(discovery.getApplication(ZOSMF)).thenReturn(application);
       when(restTemplate.exchange(Mockito.anyString(),
           Mockito.eq(HttpMethod.GET),
           Mockito.any(),
           Mockito.<Class<Object>>any()))
           .thenThrow(RestClientException.class);
       ZosmfService zosmfService = createZosmfService();
       ZosmfAuthenticationProvider zosmfAuthenticationProvider =
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);

       Exception exception = assertThrows(AuthenticationServiceException.class,
           () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
           "Expected exception is not AuthenticationServiceException");
       assertEquals("A failure occurred when authenticating.", exception.getMessage());
   }

   @Test
   void shouldThrowNewExceptionIfResourceAccessException() {
       authConfigurationProperties.setZosmfServiceId(ZOSMF);

       final Application application = createApplication(zosmfInstance);
       when(discovery.getApplication(ZOSMF)).thenReturn(application);
       when(restTemplate.exchange(Mockito.anyString(),
           Mockito.eq(HttpMethod.GET),
           Mockito.any(),
           Mockito.<Class<Object>>any()))
           .thenThrow(ResourceAccessException.class);
       ZosmfService zosmfService = createZosmfService();
       ZosmfAuthenticationProvider zosmfAuthenticationProvider =
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);

       Exception exception = assertThrows(ServiceNotAccessibleException.class,
           () -> zosmfAuthenticationProvider.authenticate(usernamePasswordAuthentication),
           "Expected exception is not ServiceNotAccessibleException");
       assertEquals("Could not get an access to z/OSMF service.", exception.getMessage());
   }

   @Test
   void shouldReturnTrueWhenSupportMethodIsCalledWithCorrectClass() {
       authConfigurationProperties.setZosmfServiceId(ZOSMF);

       final Application application = createApplication(zosmfInstance);
       when(discovery.getApplication(ZOSMF)).thenReturn(application);
       ZosmfService zosmfService = createZosmfService();
       ZosmfAuthenticationProvider zosmfAuthenticationProvider =
           new ZosmfAuthenticationProvider(authenticationService, zosmfService);

       boolean supports = zosmfAuthenticationProvider.supports(usernamePasswordAuthentication.getClass());
       assertTrue(supports);
   }

   @Test
   void testSupports() {
       ZosmfAuthenticationProvider mock = new ZosmfAuthenticationProvider(null, null);

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
       ZosmfAuthenticationProvider zosmfAuthenticationProvider = new ZosmfAuthenticationProvider(authenticationService, zosmfService);
       ReflectionTestUtils.setField(zosmfAuthenticationProvider, "useJwtToken", Boolean.TRUE);
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
       ZosmfAuthenticationProvider zosmfAuthenticationProvider = new ZosmfAuthenticationProvider(authenticationService, zosmfService);
       ReflectionTestUtils.setField(zosmfAuthenticationProvider, "useJwtToken", Boolean.FALSE);

       EnumMap<ZosmfService.TokenType, String> tokens = new EnumMap<>(ZosmfService.TokenType.class);
       tokens.put(ZosmfService.TokenType.JWT, "jwtToken");
       tokens.put(ZosmfService.TokenType.LTPA, "ltpaToken");
       when(zosmfService.authenticate(any())).thenReturn(new ZosmfService.AuthenticationResponse("domain", tokens));

       zosmfAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("userId", "password"));

       verify(authenticationService, times(1)).createJwtToken("userId", "domain", "ltpaToken");
   }

}
