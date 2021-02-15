/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.zosmf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.DiscoveryClient;
import org.hamcrest.collection.IsMapContaining;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZosmfServiceTest {

    private static final String ZOSMF_ID = "zosmf";

    @Mock
    private AuthConfigurationProperties authConfigurationProperties;
    @Mock
    private AuthenticationService authenticationService;
    private DiscoveryClient discovery = mock(DiscoveryClient.class);
    private RestTemplate restTemplate = mock(RestTemplate.class);
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    private TokenValidationStrategy tokenValidationStrategy1 = mock(TokenValidationStrategy.class);
    private TokenValidationStrategy tokenValidationStrategy2 = mock(TokenValidationStrategy.class);
    private List<TokenValidationStrategy> validationStrategyList = new ArrayList<>();

    {
        validationStrategyList.add(tokenValidationStrategy1);
        validationStrategyList.add(tokenValidationStrategy2);
    }

    @Spy
    private ObjectMapper securityObjectMapper;

    private ZosmfService getZosmfServiceSpy() {
        ZosmfService zosmfServiceObj = new ZosmfService(authConfigurationProperties,
            discovery,
            restTemplate,
            securityObjectMapper,
            applicationContext,
            null,
            authenticationService
        );
        ZosmfService zosmfService = spy(zosmfServiceObj);
        doReturn(ZOSMF_ID).when(zosmfService).getZosmfServiceId();
        doReturn("http://zosmf:1433").when(zosmfService).getURI(ZOSMF_ID);
        ReflectionTestUtils.setField(zosmfService, "meAsProxy", zosmfService);
        return zosmfService;
    }

    private ZosmfService getZosmfServiceWithValidationStrategy(List<TokenValidationStrategy> validationStrategyList) {
        ZosmfService zosmfServiceObj = new ZosmfService(authConfigurationProperties,
            discovery,
            restTemplate,
            securityObjectMapper,
            applicationContext,
            validationStrategyList,
            authenticationService);

        ZosmfService zosmfService = spy(zosmfServiceObj);
        doReturn("http://host:port").when(zosmfService).getURI(any());

        ReflectionTestUtils.setField(zosmfService, "meAsProxy", zosmfService);
        return zosmfService;
    }

    private HttpHeaders getBasicRequestHeaders() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        return addCSRFHeader(requestHeaders);
    }

    private HttpHeaders addCSRFHeader(HttpHeaders headers) {
        headers.add("X-CSRF-ZOSMF-HEADER", "");
        return headers;
    }

    @Test
    void testAuthenticateShouldUseAuthEndpointWhenAuthEndpointExists() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        doReturn(true).when(zosmfService).loginEndpointExists();

        HttpHeaders requestHeaders = getBasicRequestHeaders();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.SET_COOKIE, "jwtToken=jt");
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", responseHeaders, HttpStatus.OK);

        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.POST,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "pass");
        ZosmfService.AuthenticationResponse response = zosmfService.authenticate(authentication);

        assertNotNull(response);
        assertNotNull(response.getTokens());
        assertEquals(1, response.getTokens().size());
        assertEquals("jt", response.getTokens().get(ZosmfService.TokenType.JWT));
    }

    @Test
    void testAuthenticateShouldUseInfoEndpointWhenAuthEndpointDoesNotExist() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        doReturn(false).when(zosmfService).loginEndpointExists();
        doReturn("realm").when(zosmfService).getZosmfRealm("http://zosmf:1433/zosmf/info");
        HttpHeaders requestHeaders = getBasicRequestHeaders();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.SET_COOKIE, "LtpaToken2=lt");
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", responseHeaders, HttpStatus.OK);

        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/info",
            HttpMethod.GET,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "pass");
        ZosmfService.AuthenticationResponse response = zosmfService.authenticate(authentication);

        assertNotNull(response);
        assertNotNull(response.getTokens());
        assertEquals(1, response.getTokens().size());
        assertEquals("lt", response.getTokens().get(ZosmfService.TokenType.LTPA));
    }

    @Test
    void testAuthenticateShouldThrowException() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST)).when(restTemplate).exchange(
            anyString(),
            (HttpMethod) any(),
            (HttpEntity<?>) any(),
            (Class<?>) any()
        );
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken("user", "pass");
        assertThrows(AuthenticationServiceException.class, () -> zosmfService.authenticate(authToken));
    }

    @Test
    void testAuthenticationEndpointExists() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        prepareAuthenticationEndpoint(zosmfService, responseException);
        assertTrue(zosmfService.loginEndpointExists());
    }

    @Test
    void testAuthenticationEndpointExistsNotFoundException() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        prepareAuthenticationEndpoint(zosmfService, responseException);

        assertFalse(zosmfService.loginEndpointExists());
    }

    void prepareAuthenticationEndpoint(ZosmfService zosmfService, Exception exception) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(zosmfService.ZOSMF_CSRF_HEADER, "");

        doThrow(exception).when(restTemplate).exchange(
            anyString(),
            (HttpMethod) any(),
            (HttpEntity<?>) any(),
            (Class<?>) any()
        );
    }

    @Test
    void testInvalidateJWT() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).logoutEndpointExists();
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders = addCSRFHeader(requestHeaders);
        requestHeaders.add(HttpHeaders.COOKIE, "jwtToken=jwt");

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.OK);
        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.DELETE,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        assertDoesNotThrow(() -> zosmfService.invalidate(ZosmfService.TokenType.JWT, "jwt"));
    }

    @Test
    void handlesExceptionsFromValidationStrategy() {
        ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(Arrays.asList(tokenValidationStrategy1));

        doThrow(RuntimeException.class).when(tokenValidationStrategy1).validate(any());
        assertDoesNotThrow(() -> zosmfService.validate("TOKN"));
    }

    @Test
    void returnsResultBasedOnTokenValidationRequestStatus() {
        ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(Arrays.asList(tokenValidationStrategy1));

        //UNKNOWN by default
        assertThat(zosmfService.validate("TOKN"), is(false));

        doValidate(tokenValidationStrategy1, TokenValidationRequest.STATUS.AUTHENTICATED);

        assertThat(zosmfService.validate("TOKN"), is(true));

        doValidate(tokenValidationStrategy1, TokenValidationRequest.STATUS.INVALID);
        assertThat(zosmfService.validate("TOKN"), is(false));
    }

    @Test
    void validationHappensWithShortCircuitLogic() {
        ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(validationStrategyList);

        assertThat(zosmfService.validate("TOKN"), is(false));
        verify(tokenValidationStrategy1, times(1)).validate(any());
        verify(tokenValidationStrategy2, times(1)).validate(any());

        doValidate(tokenValidationStrategy1, TokenValidationRequest.STATUS.AUTHENTICATED);
        assertThat(zosmfService.validate("TOKN"), is(true));
        verify(tokenValidationStrategy1, times(2)).validate(any());
        verify(tokenValidationStrategy2, times(1)).validate(any());
    }

    private static void doValidate(TokenValidationStrategy tokenValidationStrategy1, TokenValidationRequest.STATUS status) {
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((TokenValidationRequest) args[0]).setAuthenticated(status);
            return null;
        }).when(tokenValidationStrategy1).validate(any());
    }

    @Test
    void doesNotRethrowExceptionsFromValidationStrategies() {
        ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(Arrays.asList(tokenValidationStrategy1));
        TokenValidationRequest request = mock(TokenValidationRequest.class);

        doThrow(RuntimeException.class).when(tokenValidationStrategy1).validate(request);
        assertDoesNotThrow(() -> zosmfService.validate("TOKN"));
    }

    @Test
    void suppliesValidationRequestWithVerifiedEndpointsList() {
        ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(validationStrategyList);
        zosmfService.validate("TOKN");
        verify(tokenValidationStrategy1).validate(argThat(request -> request.getEndpointExistenceMap().size() > 0));
    }

    @Test
    void getsEndpointMapWithGivenData() {
        ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(validationStrategyList);
        doReturn(true).when(zosmfService).loginEndpointExists();
        assertThat(zosmfService.getEndpointMap(), IsMapContaining.hasEntry("http://host:port" + ZosmfService.ZOSMF_AUTHENTICATE_END_POINT, true));
        doReturn(false).when(zosmfService).loginEndpointExists();
        assertThat(zosmfService.getEndpointMap(), IsMapContaining.hasEntry("http://host:port" + ZosmfService.ZOSMF_AUTHENTICATE_END_POINT, false));
    }


    @Test
    void testInvalidateLTPA() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).logoutEndpointExists();

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders = addCSRFHeader(requestHeaders);
        requestHeaders.add(HttpHeaders.COOKIE, "LtpaToken2=lt");

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.OK);
        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.DELETE,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        assertDoesNotThrow(() -> zosmfService.invalidate(ZosmfService.TokenType.LTPA, "lt"));
    }

    @Test
    void testInvalidateUnexpectedHttpStatusCode() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).logoutEndpointExists();

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders = addCSRFHeader(requestHeaders);
        requestHeaders.add(HttpHeaders.COOKIE, "jwtToken=jwt");

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.I_AM_A_TEAPOT);
        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.DELETE,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        try {
            zosmfService.invalidate(ZosmfService.TokenType.JWT, "jwt");
        } catch (ServiceNotAccessibleException e) {
            assertEquals("Could not get an access to z/OSMF service.", e.getMessage());
        }
    }

    @Test
    void testInvalidateRuntimeException() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).logoutEndpointExists();

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders = addCSRFHeader(requestHeaders);
        requestHeaders.add(HttpHeaders.COOKIE, "jwtToken=jwt");

        RuntimeException exception = new RuntimeException("Runtime Exception");
        doThrow(exception).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.DELETE,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        try {
            zosmfService.invalidate(ZosmfService.TokenType.JWT, "jwt");
        } catch (RuntimeException e) {
            assertEquals("Runtime Exception", e.getMessage());
        }
    }

    @Test
    void testReadTokenFromCookie() {
        assertNull(new ZosmfService(null, null, null, null, null, null, null).readTokenFromCookie(null, null));
    }

    @Test
    void testGetPublicKeys_zosm404() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        when(restTemplate.getForObject(anyString(), any()))
            .thenThrow(mock(HttpClientErrorException.NotFound.class));
        assertTrue(zosmfService.getPublicKeys().getKeys().isEmpty());
    }

    @Test
    void testGetPublicKeys_invalidFormat() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        when(restTemplate.getForObject(anyString(), any()))
            .thenReturn("invalidFormat");
        assertTrue(zosmfService.getPublicKeys().getKeys().isEmpty());
    }

    private static final String ZOSMF_PUBLIC_KEY_JSON = "{\n" +
        "    \"keys\": [\n" +
        "        {\n" +
        "            \"kty\": \"RSA\",\n" +
        "            \"e\": \"AQAB\",\n" +
        "            \"use\": \"sig\",\n" +
        "            \"kid\": \"ozG_ySMHRsVQFmN1mVBeS-WtCupY1r-K7ewben09IBg\",\n" +
        "            \"alg\": \"RS256\",\n" +
        "            \"n\": \"wRdwksGIAR2A4cHsoOsYcGp5AmQl5ZjF5xIPXeyjkaLHmNTMvjixdWso1ecVlVeg_6pIXzMRhmOvmjXjz1PLfI2GD3drmeqsStjISWdDfH_rIQCYc9wYbWIZ3bQ0wFRDaVpZ6iOZ2iNcIevvZQKNw9frJthKSMM52JtsgwrgN--Ub2cKWioU_d52SC2SfDzOdnChqlU7xkqXwKXSUqcGM92A35dJJXkwbZhAHnDy5FST1HqYq27MOLzBkChw1bJQHZtlSqkxcHPxphnnbFKQmwRVUvyC5kfBemX-7Mzp1wDogt5lGvBAf3Eq8rFxaevAke327rM7q2KqO_LDMN2J-Q\"\n" +
        "        }\n" +
        "    ]\n" +
        "}";

    @Test
    void testGetPublicKeys_success() throws JSONException {
        ZosmfService zosmfService = getZosmfServiceSpy();
        when(restTemplate.getForObject(
            "http://zosmf:1433/jwt/ibm/api/zOSMFBuilder/jwk",
            String.class
        )).thenReturn(ZOSMF_PUBLIC_KEY_JSON);

        JSONAssert.assertEquals(ZOSMF_PUBLIC_KEY_JSON, new JSONObject(zosmfService.getPublicKeys().toString()), true);
    }
}
