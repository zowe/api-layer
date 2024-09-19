/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.zosmf;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import org.hamcrest.collection.IsMapContaining;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.*;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.login.ChangePasswordRequest;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.ParsedTokenAuthSource;

import javax.management.ServiceNotFoundException;
import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.zaas.security.service.zosmf.ZosmfService.TokenType.JWT;
import static org.zowe.apiml.zaas.security.service.zosmf.ZosmfService.TokenType.LTPA;

@ExtendWith(MockitoExtension.class)
class ZosmfServiceTest {

    private static final String ZOSMF_ID = "zosmf";

    private final AuthConfigurationProperties authConfigurationProperties = mock(AuthConfigurationProperties.class);

    @Mock
    private DiscoveryClient discovery;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private TokenValidationStrategy tokenValidationStrategy1;

    @Mock
    private TokenValidationStrategy tokenValidationStrategy2;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private TokenCreationService tokenCreationService;

    private final List<TokenValidationStrategy> validationStrategyList = new ArrayList<>();

    {
        when(authConfigurationProperties.getZosmf()).thenReturn(mock(AuthConfigurationProperties.Zosmf.class));
    }

    private final ObjectMapper securityObjectMapper = spy(ObjectMapper.class);

    private ZosmfService getZosmfServiceSpy() {
        ZosmfService zosmfServiceObj = new ZosmfService(authConfigurationProperties,
            discovery,
            restTemplate,
            securityObjectMapper,
            applicationContext,
            authenticationService,
            tokenCreationService,
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
            authenticationService,
            tokenCreationService,
            validationStrategyList);

        ZosmfService zosmfService = spy(zosmfServiceObj);
        doReturn("http://host:1433").when(zosmfService).getURI(any());

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

    @Nested
    class GivenValidCredentials {

        @Nested
        class WhenAuthEndpointExists {
            @Test
            void thenUseAuthEndpoint() {
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
                assertEquals("jt", response.getTokens().get(JWT));
            }
        }

        @Nested
        class WhenAuthEndpointDoenstExists {
            @Test
            void thenShouldThrowException() {
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
        }

        @Nested
        class WhenTokenIsInvalidated {
            @Test
            void thenThrowTokenException() {
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
        }

        @Nested
        class WhenLTPATokenIsInvalidated {
            @Test
            void thenThrowTokenException() {
                Authentication authentication = new UsernamePasswordAuthenticationToken("user", "pass");
                ZosmfService zosmfService = getZosmfServiceSpy();
                doReturn(true).when(zosmfService).loginEndpointExists();
                ZosmfService.AuthenticationResponse responseMock = mock(ZosmfService.AuthenticationResponse.class);
                doReturn(responseMock).when(zosmfService).issueAuthenticationRequest(any(), any(), any());

                doReturn(true).when(zosmfService).isInvalidated(any());
                assertThrows(TokenNotValidException.class, () -> zosmfService.authenticate(authentication));
                verify(zosmfService, times(1)).invalidate(eq(LTPA), any());
            }
        }
    }

    @Nested
    class GivenLoginRequestCredentials {
        @Nested
        class WhenAuthenticate {
            @Test
            void thenAuthenticateWithSuccess() {
                Authentication authentication = mock(UsernamePasswordAuthenticationToken.class);
                LoginRequest loginRequest = mock(LoginRequest.class);
                ZosmfService zosmfService = getZosmfServiceSpy();
                when(authentication.getCredentials()).thenReturn(loginRequest);
                doReturn(true).when(zosmfService).loginEndpointExists();

                doReturn(new ZosmfService.AuthenticationResponse(
                    "domain1",
                    Collections.singletonMap(JWT, "jwtToken1"))).when(zosmfService).getAuthenticationResponse(any());

                when(loginRequest.getPassword()).thenReturn("password".toCharArray());
                when(authentication.getPrincipal()).thenReturn("principal");

                ZosmfService.AuthenticationResponse response = zosmfService.authenticate(authentication);

                assertNotNull(response);
                assertNotNull(response.getTokens());
                assertEquals(1, response.getTokens().size());
                assertEquals("jwtToken1", response.getTokens().get(JWT));
            }
        }

        @Nested
        class WhenChangingPassword {

            private final LoginRequest loginRequest;
            private final Authentication authentication;

            private final HttpHeaders requiredHeaders;
            private ZosmfService zosmfService;

            {
                requiredHeaders = new HttpHeaders();
                requiredHeaders.add("X-CSRF-ZOSMF-HEADER", "");
                requiredHeaders.setContentType(MediaType.APPLICATION_JSON);

                loginRequest = new LoginRequest("username", "password".toCharArray(), "newPassword".toCharArray());
                authentication = mock(UsernamePasswordAuthenticationToken.class);
            }

            @BeforeEach
            void setUp() {
                this.zosmfService = getZosmfServiceSpy();
                when(authentication.getCredentials()).thenReturn(loginRequest);
            }

            @Test
            void thenChangePasswordWithSuccess() {
                ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", null, HttpStatus.OK);
                doReturn(responseEntity).when(zosmfService).issueChangePasswordRequest(any(), any(), any());
                ResponseEntity<?> response = zosmfService.changePassword(authentication);

                assertTrue(response.getStatusCode().is2xxSuccessful());
            }

            @Nested
            class WhenClientError {

                @Test
                void thenChangePasswordWithClientError() {
                    when(restTemplate.exchange("http://zosmf:1433/zosmf/services/authenticate",
                        HttpMethod.PUT,
                        new HttpEntity<>(new ChangePasswordRequest(loginRequest), requiredHeaders),
                        String.class))
                        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

                    assertThrows(BadCredentialsException.class, () -> zosmfService.changePassword(authentication));
                }

                @Test
                void thenChangePasswordWithUnsupportedZosmf() {
                    when(restTemplate.exchange("http://zosmf:1433/zosmf/services/authenticate",
                        HttpMethod.PUT,
                        new HttpEntity<>(new ChangePasswordRequest(loginRequest), requiredHeaders),
                        String.class))
                        .thenThrow(HttpClientErrorException.create(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", null, null, null));

                    assertThrows(ServiceNotAccessibleException.class, () -> zosmfService.changePassword(authentication));
                }
            }

            @Nested
            class WhenServerError {
                @Test
                void thenChangePasswordWithServerError() {
                    when(restTemplate.exchange("http://zosmf:1433/zosmf/services/authenticate",
                        HttpMethod.PUT,
                        new HttpEntity<>(new ChangePasswordRequest(loginRequest), requiredHeaders),
                        String.class))
                        .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

                    assertThrows(AuthenticationServiceException.class, () -> zosmfService.changePassword(authentication));
                }

                @Test
                void thenChangePasswordWithZosmfInternalError() {
                    when(restTemplate.exchange("http://zosmf:1433/zosmf/services/authenticate",
                        HttpMethod.PUT,
                        new HttpEntity<>(new ChangePasswordRequest(loginRequest), requiredHeaders),
                        String.class))
                        .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "{\"returnCode\": 4}".getBytes(), Charset.defaultCharset()));

                    assertThrows(AuthenticationServiceException.class, () -> zosmfService.changePassword(authentication));
                }

                @Test
                void thenChangePasswordWithZosmfValidationError() {
                    when(restTemplate.exchange("http://zosmf:1433/zosmf/services/authenticate",
                        HttpMethod.PUT,
                        new HttpEntity<>(new ChangePasswordRequest(loginRequest), requiredHeaders),
                        String.class))
                        .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "{\"returnCode\": 8}".getBytes(), Charset.defaultCharset()));

                    assertThrows(BadCredentialsException.class, () -> zosmfService.changePassword(authentication));
                }
            }
        }
    }

    @Nested
    class TokenInResponse {
        @Nested
        class WhenAuthEndpointNotExists {
            @Test
            void thenProvidedUsernamePassword() {
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
            void thenProvidedLoginRequest() {
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

                Authentication authentication = new UsernamePasswordAuthenticationToken("user", new LoginRequest("user", "pass".toCharArray()));
                ZosmfService.AuthenticationResponse response = zosmfService.authenticate(authentication);

                assertNotNull(response);
                assertNotNull(response.getTokens());
                assertEquals(1, response.getTokens().size());
                assertEquals("lt", response.getTokens().get(ZosmfService.TokenType.LTPA));
            }
        }
    }

    @Nested
    class GivenResponseException {
        @Test
        void whenUnauthorized_thenAuthenticationEndpointExists() {
            ZosmfService zosmfService = getZosmfServiceSpy();

            HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
            prepareZosmfEndpoint(responseException);
            assertTrue(zosmfService.loginEndpointExists());
        }

        @Test
        void whenNotFound_thenAuthenticationEndpointExists() {
            ZosmfService zosmfService = getZosmfServiceSpy();

            HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
            prepareZosmfEndpoint(responseException);

            assertFalse(zosmfService.loginEndpointExists());
        }

        @Test
        void whenUnauthorized_thenJwtEndpointExists() {
            ZosmfService zosmfService = getZosmfServiceSpy();

            HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
            prepareZosmfEndpoint(responseException);
            assertTrue(zosmfService.jwtBuilderEndpointExists());
        }

        @Test
        void whenUnknownClientException_thenJwtEndpointExists() {
            ZosmfService zosmfService = getZosmfServiceSpy();

            HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
            prepareZosmfEndpoint(responseException);
            assertFalse(zosmfService.jwtBuilderEndpointExists());
        }

        @Test
        void whenServerException_thenJwtEndpointExists() {
            ZosmfService zosmfService = getZosmfServiceSpy();

            HttpServerErrorException responseException = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
            prepareZosmfEndpoint(responseException);
            assertFalse(zosmfService.jwtBuilderEndpointExists());
        }

        @Test
        void whenRandomException_thenJwtEndpointExists() {
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
    }

    @Nested
    class WhenValidateToken {

        @BeforeEach
        void setUp() {
            validationStrategyList.add(tokenValidationStrategy1);
            validationStrategyList.add(tokenValidationStrategy2);
        }

        @Test
        void givenException_thenHandleExceptions() {
            ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(Collections.singletonList(tokenValidationStrategy1));

            doThrow(RuntimeException.class).when(tokenValidationStrategy1).validate(any());
            assertDoesNotThrow(() -> zosmfService.validate("TOKN"));
        }

        @Test
        void givenOneValidationStrategy_thenReturnValidationStrategyResult() {
            ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(Collections.singletonList(tokenValidationStrategy1));

            //UNKNOWN by default
            assertThat(zosmfService.validate("TOKN"), is(false));

            doValidate(tokenValidationStrategy1, TokenValidationRequest.STATUS.AUTHENTICATED);

            assertThat(zosmfService.validate("TOKN"), is(true));

            doValidate(tokenValidationStrategy1, TokenValidationRequest.STATUS.INVALID);
            assertThat(zosmfService.validate("TOKN"), is(false));
        }

        @Test
        void givenFirstValidationStrategyAuthentications_thenDontUseSecondValidationStrategy() {
            ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(validationStrategyList);

            assertThat(zosmfService.validate("TOKN"), is(false));
            verify(tokenValidationStrategy1, times(1)).validate(any());
            verify(tokenValidationStrategy2, times(1)).validate(any());

            doValidate(tokenValidationStrategy1, TokenValidationRequest.STATUS.AUTHENTICATED);
            assertThat(zosmfService.validate("TOKN"), is(true));
            verify(tokenValidationStrategy1, times(2)).validate(any());
            verify(tokenValidationStrategy2, times(1)).validate(any());
        }

        @Test
        void givenFirstStrategyInvalidAndSecondValid_thenTokenIsValid() {
            ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(validationStrategyList);

            doValidate(tokenValidationStrategy1, TokenValidationRequest.STATUS.INVALID);
            doValidate(tokenValidationStrategy2, TokenValidationRequest.STATUS.AUTHENTICATED);

            assertThat(zosmfService.validate("TOKN"), is(true));
            verify(tokenValidationStrategy1, times(1)).validate(any());
            verify(tokenValidationStrategy2, times(1)).validate(any());
        }

        @Test
        void doesNotRethrowExceptionsFromValidationStrategies() {
            ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(Collections.singletonList(tokenValidationStrategy1));
            TokenValidationRequest request = mock(TokenValidationRequest.class);

            lenient().doThrow(RuntimeException.class).when(tokenValidationStrategy1).validate(request);
            assertDoesNotThrow(() -> zosmfService.validate("TOKN"));
        }

        @Test
        void suppliesValidationRequestWithVerifiedEndpointsList() {
            ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(validationStrategyList);
            zosmfService.validate("TOKN");
            verify(tokenValidationStrategy1).validate(argThat(request -> !request.getEndpointExistenceMap().isEmpty()));
        }

        private void doValidate(TokenValidationStrategy tokenValidationStrategy1, TokenValidationRequest.STATUS status) {
            doAnswer(invocation -> {
                Object[] args = invocation.getArguments();
                ((TokenValidationRequest) args[0]).setAuthenticated(status);
                return null;
            }).when(tokenValidationStrategy1).validate(any());
        }
    }

    @Nested
    class WhenGetsEndpointMap {
        @Test
        void thenReturnGivenData() {
            ZosmfService zosmfService = getZosmfServiceWithValidationStrategy(validationStrategyList);
            doReturn(true).when(zosmfService).loginEndpointExists();
            assertThat(zosmfService.getEndpointMap(), IsMapContaining.hasEntry("http://host:1433" + ZosmfService.ZOSMF_AUTHENTICATE_END_POINT, true));
            doReturn(false).when(zosmfService).loginEndpointExists();
            assertThat(zosmfService.getEndpointMap(), IsMapContaining.hasEntry("http://host:1433" + ZosmfService.ZOSMF_AUTHENTICATE_END_POINT, false));
        }
    }

    @Nested
    class WhenInvalidateToken {
        @Test
        void thenInvalidateWithSuccess() {
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

            assertDoesNotThrow(() -> zosmfService.invalidate(JWT, "jwt"));
        }

        @Test
        void thenInvalidateLTPA() {
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
        void thenTestInvalidateUnexpectedHttpStatusCode() {
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
                zosmfService.invalidate(JWT, "jwt");
            } catch (ServiceNotAccessibleException e) {
                assertEquals("Could not get an access to z/OSMF service.", e.getMessage());
            }
        }

        @Test
        void thenTestInvalidateRuntimeException() {
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
                zosmfService.invalidate(JWT, "jwt");
            } catch (RuntimeException e) {
                assertEquals("Runtime Exception", e.getMessage());
            }
        }

    }

    @Nested
    class WhenReadTokenFromCookie {
        private static final String ZOSMF_PUBLIC_KEY_JSON =
            """
                  {
                    "keys": [
                      {
                        "kty": "RSA",
                        "e": "AQAB",
                        "use": "sig",
                        "kid": "ozG_ySMHRsVQFmN1mVBeS-WtCupY1r-K7ewben09IBg",
                        "alg": "RS256",
                        "n": "wRdwksGIAR2A4cHsoOsYcGp5AmQl5ZjF5xIPXeyjkaLHmNTMvjixdWso1ecVlVeg_6pIXzMRhmOvmjXjz1PLfI2GD3drmeqsStjISWdDfH_rIQCYc9wYbWIZ3bQ0wFRDaVpZ6iOZ2iNcIevvZQKNw9frJthKSMM52JtsgwrgN--Ub2cKWioU_d52SC2SfDzOdnChqlU7xkqXwKXSUqcGM92A35dJJXkwbZhAHnDy5FST1HqYq27MOLzBkChw1bJQHZtlSqkxcHPxphnnbFKQmwRVUvyC5kfBemX-7Mzp1wDogt5lGvBAf3Eq8rFxaevAke327rM7q2KqO_LDMN2J-Q"
                      }
                    ]
                  }
                """;

        @Test
        void thenSuccess() throws JSONException, ParseException {
            String zosmfJwtUrl = "/jwt/ibm/api/zOSMFBuilder/jwk";
            when(authConfigurationProperties.getZosmf().getJwtEndpoint()).thenReturn(zosmfJwtUrl);
            ZosmfService zosmfService = getZosmfServiceSpy();
            JWKSet jwkSet = JWKSet.parse(ZOSMF_PUBLIC_KEY_JSON);
            try (MockedStatic<JWKSet> mockedStatic = Mockito.mockStatic(JWKSet.class)) {
                mockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(jwkSet);
                JSONAssert.assertEquals(ZOSMF_PUBLIC_KEY_JSON, new JSONObject(zosmfService.getPublicKeys().toString()), true);
            }
        }

        @Test
        void thenReturnNull() {
            assertNull(new ZosmfService(null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)
                .readTokenFromCookie(null, null));
        }
    }

    @Nested
    class WhenGetsPublicKeys {

        @Test
        void givenExceptionInTheResponse_thenPublicKeysAreEmpty() {
            ZosmfService zosmfService = getZosmfServiceSpy();
            assertTrue(zosmfService.getPublicKeys().getKeys().isEmpty());
        }
    }

    @Nested
    class WhenTestingIfTheZosmfIsAvailable {
        private ZosmfService underTest;
        private final String ZOSMF_URL = "http://host:1433";

        @BeforeEach
        void setUp() {
            when(authConfigurationProperties.validatedZosmfServiceId()).thenReturn(ZOSMF_ID);

            ZosmfService zosmfService = new ZosmfService(
                authConfigurationProperties,
                discovery,
                restTemplate,
                securityObjectMapper,
                applicationContext,
                authenticationService,
                tokenCreationService,
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

        @Nested
        class WhenZosmfIsNotAvailable {

            @Mock
            private Appender<ILoggingEvent> mockedAppender;
            @Mock
            private ApimlLogger apimlLogger;

            @Captor
            private ArgumentCaptor<LoggingEvent> loggingCaptor;

            private Logger logger;

            @BeforeEach
            void setUp() {
                logger = (Logger) LoggerFactory.getLogger(AbstractZosmfService.class);
                logger.detachAndStopAllAppenders();
                logger.getLoggerContext().resetTurboFilterList();
                logger.addAppender(mockedAppender);
                logger.setLevel(Level.TRACE);

                ReflectionTestUtils.setField(underTest, "apimlLog", apimlLogger);
            }

            @AfterEach
            void tearDown() {
                logger.detachAppender(mockedAppender);
            }

            private String loggedValues() {
                List<LoggingEvent> values = loggingCaptor.getAllValues();
                assertNotNull(values);
                assertFalse(values.isEmpty());
                return values.stream().map(LoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
            }

            @Test
            void givenGetURIFails_thenFalseIsReturned() {
                when(underTest.getURI(any())).thenThrow(new ServiceNotAccessibleException("z/OSMF not accessible"));
                assertThat(underTest.isAccessible(), is(false));
                verifyNoInteractions(restTemplate);
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
                verify(mockedAppender, atLeast(1)).doAppend(loggingCaptor.capture());
                String values = loggedValues();
                assertFalse(values.isEmpty());
                assertTrue(values.contains("z/OSMF isn't accessible"), values);
            }

            @Test
            void givenSSLError_thenFalseAndException() {
                when(restTemplate.exchange(
                    eq(ZOSMF_URL + AbstractZosmfService.ZOSMF_INFO_END_POINT),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(ZosmfService.ZosmfInfo.class)
                )).thenThrow(new ResourceAccessException("resource access exception", new SSLHandshakeException("handshake exception")));

                assertThat(underTest.isAccessible(), is(false));
                verify(mockedAppender, atLeast(1)).doAppend(loggingCaptor.capture());
                String values = loggedValues();
                assertFalse(values.isEmpty());
                assertTrue(values.contains("ResourceAccessException accessing"), values);

                verify(apimlLogger, times(1)).log("org.zowe.apiml.security.auth.zosmf.sslError", "resource access exception");
            }

            @Test
            void givenConnectionIssue_thenFalseAndException() {
                when(restTemplate.exchange(
                    eq(ZOSMF_URL + AbstractZosmfService.ZOSMF_INFO_END_POINT),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(ZosmfService.ZosmfInfo.class)
                )).thenThrow(new RestClientException("resource access exception", new ConnectException("connection exception")));

                assertThat(underTest.isAccessible(), is(false));
                verify(apimlLogger, times(1)).log("org.zowe.apiml.security.auth.zosmf.connectError", "resource access exception");
            }

            @Test
            void givenUnexpectedStatusCode_thenFalseAndException() {
                when(restTemplate.exchange(
                    eq(ZOSMF_URL + AbstractZosmfService.ZOSMF_INFO_END_POINT),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(ZosmfService.ZosmfInfo.class)
                )).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));
                assertThat(underTest.isAccessible(), is(false));
            }
        }
    }

    @Nested
    class WhenVerifyingAuthenticationEndpoint {

        private final String ZOSMF_URL = "http://host:1433";

        private ZosmfService underTest;

        @BeforeEach
        void setUp() {
            when(authConfigurationProperties.validatedZosmfServiceId()).thenReturn(ZOSMF_ID);

            ZosmfService zosmfService = new ZosmfService(
                authConfigurationProperties,
                discovery,
                restTemplate,
                securityObjectMapper,
                applicationContext,
                authenticationService,
                tokenCreationService,
                null
            );

            underTest = spy(zosmfService);
            doReturn(ZOSMF_URL).when(underTest).getURI(any());
        }

        @Nested
        class GivenZosmfIsNotAvailable {
            @Test
            void givenGetURIFails_thenFalseReturned() {
                when(underTest.getURI(any())).thenThrow(new ServiceNotAccessibleException("z/OSMF not accessible"));
                assertFalse(underTest.authenticationEndpointExists(null, getBasicRequestHeaders()));
                verifyNoInteractions(restTemplate);
            }
        }
    }

    @Nested
    class WhenExchangingAuthenticationForZosmfToken {

        private static final String USER = "user";
        private static final String ZOWE_JWT_TOKEN = "zoweToken";
        private static final String ZOSMF_JWT_TOKEN = "zosmfToken";
        private static final String LTPA_TOKEN = "ltpaToken";

        private ZosmfService underTest;

        @BeforeEach
        void setup() {
            underTest = new ZosmfService(
                authConfigurationProperties,
                discovery,
                restTemplate,
                securityObjectMapper,
                applicationContext,
                authenticationService,
                tokenCreationService,
                null
            );
        }

        @Test
        void givenZosmfAuthSource_thenSameTokenIsReturned() throws ServiceNotFoundException {
            AuthSource.Parsed authParsedSource = new ParsedTokenAuthSource(USER, new Date(), new Date(), AuthSource.Origin.ZOSMF);

            ZaasTokenResponse zaasTokenResponse = underTest.exchangeAuthenticationForZosmfToken(ZOSMF_JWT_TOKEN, authParsedSource);

            assertEquals(ZOSMF_JWT_TOKEN, zaasTokenResponse.getToken());
            assertEquals(JWT.getCookieName(), zaasTokenResponse.getCookieName());
        }

        @Test
        void givenZoweAuthSourceWithLtpa_thenSameLtpaTokenIsReturned() throws ServiceNotFoundException {
            AuthSource.Parsed authParsedSource = new ParsedTokenAuthSource(USER, new Date(), new Date(), AuthSource.Origin.ZOWE);

            when(authenticationService.getLtpaToken(ZOWE_JWT_TOKEN)).thenReturn(LTPA_TOKEN);

            ZaasTokenResponse zaasTokenResponse = underTest.exchangeAuthenticationForZosmfToken(ZOWE_JWT_TOKEN, authParsedSource);

            assertEquals(LTPA_TOKEN, zaasTokenResponse.getToken());
            assertEquals(LTPA.getCookieName(), zaasTokenResponse.getCookieName());
        }

        @Test
        void givenZoweAuthSourceWithoutLtpa_thenNewJwtTokenIsReturned() throws ServiceNotFoundException {
            AuthSource.Parsed authParsedSource = new ParsedTokenAuthSource(USER, new Date(), new Date(), AuthSource.Origin.ZOWE);

            Map<ZosmfService.TokenType, String> tokens = new HashMap<>() {{
                put(JWT, ZOSMF_JWT_TOKEN);
            }};
            when(authenticationService.getLtpaToken(ZOWE_JWT_TOKEN)).thenReturn(null);
            when(tokenCreationService.createZosmfTokensWithoutCredentials(USER)).thenReturn(tokens);

            ZaasTokenResponse zaasTokenResponse = underTest.exchangeAuthenticationForZosmfToken(ZOWE_JWT_TOKEN, authParsedSource);

            assertEquals(ZOSMF_JWT_TOKEN, zaasTokenResponse.getToken());
            assertEquals(JWT.getCookieName(), zaasTokenResponse.getCookieName());
        }

        @Test
        void givenOtherAuthSourceAndZosmfProducesJwt_thenNewJwtTokenIsReturned() throws ServiceNotFoundException {
            AuthSource.Parsed authParsedSource = new ParsedTokenAuthSource(USER, new Date(), new Date(), AuthSource.Origin.OIDC);

            Map<ZosmfService.TokenType, String> tokens = new HashMap<>() {{
                put(LTPA, LTPA_TOKEN);
                put(JWT, ZOSMF_JWT_TOKEN);
            }};
            when(tokenCreationService.createZosmfTokensWithoutCredentials(USER)).thenReturn(tokens);

            ZaasTokenResponse zaasTokenResponse = underTest.exchangeAuthenticationForZosmfToken("OidcToken", authParsedSource);

            assertEquals(ZOSMF_JWT_TOKEN, zaasTokenResponse.getToken());
            assertEquals(JWT.getCookieName(), zaasTokenResponse.getCookieName());
        }

        @Test
        void givenOtherAuthSourceAndZosmfProducesOnlyLtpa_thenNewLtpaTokenIsReturned() throws ServiceNotFoundException {
            AuthSource.Parsed authParsedSource = new ParsedTokenAuthSource(USER, new Date(), new Date(), AuthSource.Origin.OIDC);

            Map<ZosmfService.TokenType, String> tokens = new HashMap<>() {{
                put(LTPA, LTPA_TOKEN);
            }};
            when(tokenCreationService.createZosmfTokensWithoutCredentials(USER)).thenReturn(tokens);

            ZaasTokenResponse zaasTokenResponse = underTest.exchangeAuthenticationForZosmfToken("OidcToken", authParsedSource);

            assertEquals(LTPA_TOKEN, zaasTokenResponse.getToken());
            assertEquals(LTPA.getCookieName(), zaasTokenResponse.getCookieName());
        }

        @Test
        void givenOtherAuthSourceAndZosmaIsNotAvailable_thenExceptionIsThrown() {
            AuthSource.Parsed authParsedSource = new ParsedTokenAuthSource(USER, new Date(), new Date(), AuthSource.Origin.OIDC);

            when(tokenCreationService.createZosmfTokensWithoutCredentials(USER)).thenReturn(Collections.emptyMap());

            assertThrows(ServiceNotFoundException.class, () -> underTest.exchangeAuthenticationForZosmfToken("OidcToken", authParsedSource));
        }

    }

}
