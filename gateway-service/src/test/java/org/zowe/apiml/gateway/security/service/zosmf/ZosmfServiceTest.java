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
import org.junit.jupiter.api.*;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.*;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.gateway.security.service.zosmf.ZosmfService.TokenType.LTPA;

class ZosmfServiceTest {

    private static final String ZOSMF_ID = "zosmf";

    private final AuthConfigurationProperties authConfigurationProperties = mock(AuthConfigurationProperties.class);
    private final DiscoveryClient discovery = mock(DiscoveryClient.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    private final TokenValidationStrategy tokenValidationStrategy1 = mock(TokenValidationStrategy.class);
    private final TokenValidationStrategy tokenValidationStrategy2 = mock(TokenValidationStrategy.class);
    private final List<TokenValidationStrategy> validationStrategyList = new ArrayList<>();

    {
        when(authConfigurationProperties.getZosmf()).thenReturn(mock(AuthConfigurationProperties.Zosmf.class));
        validationStrategyList.add(tokenValidationStrategy1);
        validationStrategyList.add(tokenValidationStrategy2);
    }

    private final ObjectMapper securityObjectMapper = spy(ObjectMapper.class);

    private ZosmfService getZosmfServiceSpy() {
        ZosmfService zosmfServiceObj = new ZosmfService(authConfigurationProperties,
            discovery,
            restTemplate,
            securityObjectMapper,
            applicationContext,
            null);
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
            validationStrategyList);

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
    void newTokenIsCheckedAgainstInvalidateTokensCache() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "pass");
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).loginEndpointExists();
        ZosmfService.AuthenticationResponse responseMock = mock(ZosmfService.AuthenticationResponse.class);
        doReturn(responseMock).when(zosmfService).issueAuthenticationRequest(any(), any(), any());

        doReturn(true).when(zosmfService).isInvalidated(any());
        assertThrows(TokenNotValidException.class, () -> zosmfService.authenticate(authentication));

        doReturn(false).when(zosmfService).isInvalidated(any());
        assertDoesNotThrow(() -> zosmfService.authenticate(authentication));
    }

    @Test
    void givenLoginRequestCredentials_whenAuthenticate_thenAuthenticateWithSuccess() {
        Authentication authentication = mock(UsernamePasswordAuthenticationToken.class);
        LoginRequest loginRequest = mock(LoginRequest.class);
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).loginEndpointExists();
        ZosmfService.AuthenticationResponse responseMock = mock(ZosmfService.AuthenticationResponse.class);
        when(authentication.getCredentials()).thenReturn(loginRequest);
        doReturn(true).when(zosmfService).loginEndpointExists();

        HttpHeaders requestHeaders = getBasicRequestHeaders();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.SET_COOKIE, "jwtToken=jt");
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", responseHeaders, HttpStatus.OK);
        doReturn(new ZosmfService.AuthenticationResponse(
            "domain1",
            Collections.singletonMap(ZosmfService.TokenType.JWT, "jwtToken1"))).when(zosmfService).getAuthenticationResponse(any());
        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.POST,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );
        when(loginRequest.getPassword()).thenReturn("password");
        when(authentication.getPrincipal()).thenReturn("principal");
        doReturn(responseMock).when(zosmfService).issueAuthenticationRequest(authentication, eq(any()), any());

        ZosmfService.AuthenticationResponse response = zosmfService.authenticate(authentication);

        assertNotNull(response);
        assertNotNull(response.getTokens());
        assertEquals(1, response.getTokens().size());
        assertEquals("jwtToken1", response.getTokens().get(ZosmfService.TokenType.JWT));
    }

    @Test
    void whenNewTokenFoundInInvalidatedTokensCacheItsLTPAtokenIsInvalidatedAgainstZosmf() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "pass");
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).loginEndpointExists();
        ZosmfService.AuthenticationResponse responseMock = mock(ZosmfService.AuthenticationResponse.class);
        doReturn(responseMock).when(zosmfService).issueAuthenticationRequest(any(), any(), any());

        doReturn(true).when(zosmfService).isInvalidated(any());
        assertThrows(TokenNotValidException.class, () -> zosmfService.authenticate(authentication));
        verify(zosmfService, times(1)).invalidate(eq(LTPA), any());
    }

    @Nested
    class TokenInResponse {
        @Nested
        class WhenAuthEndpointNotExists {
            @Test
            void providedUsernamePassword() {
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
            void providedLoginRequest() {
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

                Authentication authentication = new UsernamePasswordAuthenticationToken("user", new LoginRequest("user","pass"));
                ZosmfService.AuthenticationResponse response = zosmfService.authenticate(authentication);

                assertNotNull(response);
                assertNotNull(response.getTokens());
                assertEquals(1, response.getTokens().size());
                assertEquals("lt", response.getTokens().get(ZosmfService.TokenType.LTPA));
            }
        }
    }




    @Test
    void testAuthenticateShouldThrowException() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST)).when(restTemplate).exchange(
            anyString(),
            any(),
            any(),
            (Class<?>) any()
        );
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken("user", "pass");
        assertThrows(AuthenticationServiceException.class, () -> zosmfService.authenticate(authToken));
    }

    @Test
    void testAuthenticationEndpointExists() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        prepareZosmfEndpoint(responseException);
        assertTrue(zosmfService.loginEndpointExists());
    }

    @Test
    void testAuthenticationEndpointExistsNotFoundException() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        prepareZosmfEndpoint(responseException);

        assertFalse(zosmfService.loginEndpointExists());
    }

    @Test
    void testJwtEndpointExists() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        prepareZosmfEndpoint(responseException);
        assertTrue(zosmfService.jwtBuilderEndpointExists());
    }

    @Test
    void testJwtEndpointExistsNotFoundException() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        prepareZosmfEndpoint(responseException);
        assertTrue(zosmfService.jwtBuilderEndpointExists());
    }

    @Test
    void testJwtEndpointExistsUnknownClientException() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        prepareZosmfEndpoint(responseException);
        assertFalse(zosmfService.jwtBuilderEndpointExists());
    }

    @Test
    void testJwtEndpointExistsServerException() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        HttpServerErrorException responseException = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        prepareZosmfEndpoint(responseException);
        assertFalse(zosmfService.jwtBuilderEndpointExists());
    }

    @Test
    void testJwtEndpointExistsRandomException() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        Exception responseException = new IllegalArgumentException("This cannot happen, right?");
        prepareZosmfEndpoint(responseException);
        assertFalse(zosmfService.jwtBuilderEndpointExists());
    }

    void prepareZosmfEndpoint(Exception exception) {
        doThrow(exception).when(restTemplate).exchange(
            anyString(),
            any(),
            any(),
            (Class<?>) any()
        );
    }

    @Test
    void testInvalidateJWT() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).logoutEndpointExists();
        HttpHeaders requestHeaders = addCSRFHeader(new HttpHeaders());
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
        ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(Collections.singletonList(tokenValidationStrategy1));

        doThrow(RuntimeException.class).when(tokenValidationStrategy1).validate(any());
        assertDoesNotThrow(() -> zosmfService.validate("TOKN"));
    }

    @Test
    void returnsResultBasedOnTokenValidationRequestStatus() {
        ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(Collections.singletonList(tokenValidationStrategy1));

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
        ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(Collections.singletonList(tokenValidationStrategy1));
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

        HttpHeaders requestHeaders = addCSRFHeader(new HttpHeaders());
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

        HttpHeaders requestHeaders = addCSRFHeader(new HttpHeaders());
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

        HttpHeaders requestHeaders = addCSRFHeader(new HttpHeaders());
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
        assertNull(new ZosmfService(null, null, null, null, null, null).readTokenFromCookie(null, null));
    }

    @Test
    void testGetPublicKeys_zosm404() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        when(restTemplate.getForObject(anyString(), any()))
            .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND,
                "", new HttpHeaders(), new byte[]{}, null));
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
        String zosmfJwtUrl = "/jwt/ibm/api/zOSMFBuilder/jwk";
        when(authConfigurationProperties.getZosmf().getJwtEndpoint()).thenReturn(zosmfJwtUrl);
        ZosmfService zosmfService = getZosmfServiceSpy();
        when(restTemplate.getForObject(
            "http://zosmf:1433" + zosmfJwtUrl,
            String.class
        )).thenReturn(ZOSMF_PUBLIC_KEY_JSON);

        JSONAssert.assertEquals(ZOSMF_PUBLIC_KEY_JSON, new JSONObject(zosmfService.getPublicKeys().toString()), true);
    }

    @Nested
    class WhenTestingIfTheZosmfIsAvailable {
        private ZosmfService underTest;
        private final String ZOSMF_URL = "http://host:port";

        @BeforeEach
        void setUp() {
            when(authConfigurationProperties.validatedZosmfServiceId()).thenReturn(ZOSMF_ID);

            ZosmfService zosmfService = new ZosmfService(
                authConfigurationProperties,
                discovery,
                restTemplate,
                securityObjectMapper,
                applicationContext,
                null
            );

            underTest = spy(zosmfService);
            doReturn(ZOSMF_URL).when(underTest).getURI(any());
        }


        @Test
        void givenZosmfIsAvailable_thenTrueIsReturned() {
            when(restTemplate.exchange(
                eq(ZOSMF_URL + AbstractZosmfService.ZOSMF_INFO_END_POINT),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(ZosmfService.ZosmfInfo.class)
            )).thenReturn(new ResponseEntity<>(HttpStatus.OK));

            assertThat(underTest.isAccessible(), is(true));
        }

        @Test
        void givenZosmfIsUnavailable_thenFalseIsReturned() {
            when(restTemplate.exchange(
                eq(ZOSMF_URL + AbstractZosmfService.ZOSMF_INFO_END_POINT),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(ZosmfService.ZosmfInfo.class)
            )).thenThrow(RestClientException.class);

            assertThat(underTest.isAccessible(), is(false));
        }
    }
}
