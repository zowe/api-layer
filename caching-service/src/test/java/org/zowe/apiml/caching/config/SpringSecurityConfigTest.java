/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.caching.CachingServiceApplication;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;
import reactor.test.StepVerifier;

import java.util.Base64;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SpringSecurityConfigTest {

    private static final String USER = "user";
    private static final String PASSWORD = "password";

    private static final AbstractAuthenticationToken VALID_AUTHENTICATION = new UsernamePasswordAuthenticationToken(USER, PASSWORD.toCharArray(), Collections.singleton(new SimpleGrantedAuthority("CACHING_SERVICE")));
    private static final String validBasicAuth = "Basic " + Base64.getEncoder().encodeToString((USER + ":" + PASSWORD).getBytes());
    private static final String invalidBasicAuth = "Basic " + Base64.getEncoder().encodeToString((USER + ":invalidPassword").getBytes());
    private static final String serviceIdHeader = "X-CS-Service-ID";

    @BeforeAll
    static void init() throws Exception {
        SslContext.reset();
        RestAssured.useRelaxedHTTPSValidation();
        SslContextConfigurer configurer = new SslContextConfigurer("password".toCharArray(), "../keystore/client_cert/client-certs.p12", "../keystore/localhost/localhost.keystore.p12");
        SslContext.prepareSslAuthentication(configurer);
    }

    private String getUri(String hostname, int port) {
        return String.format("%s://%s:%d/%s", "https", hostname, port, "cachingservice/api/v1/cache");
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(
        properties = {
            "apiml.service.ssl.verifySslCertificatesOfServices=false",
            "apiml.service.http.userId=user",
            "apiml.service.http.password=password"
        }
    )
    @SpringBootTest(
        classes = CachingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenDisabledSSLVerificationAndCachingCredentials {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @Nested
        class givenNoBasicAuth {

            @Test
            void thenReturnForbidden() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

        }

        @Nested
        class givenBasicAuth {

            @Test
            void whenValidBasicAuth_thenReturnSuccess() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, validBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnUnauthorized() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, invalidBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
            }
        }
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(
        properties = {
            "apiml.service.ssl.verifySslCertificatesOfServices=false"
        }
    )
    @SpringBootTest(
        classes = CachingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenDisabledSSLVerificationAndNoCachingCredentials {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @Nested
        class givenNoBasicAuth {

            @Test
            void thenReturnForbidden() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

        }

        @Nested
        class givenBasicAuth {

            @Test
            void whenValidBasicAuth_thenReturnUnauthorized() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, validBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnUnauthorized() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, invalidBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
            }
        }
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(
        properties = {
            "apiml.service.ssl.verifySslCertificatesOfServices=true",
            "apiml.service.http.userId=user",
            "apiml.service.http.password=password"
        }
    )
    @SpringBootTest(
        classes = CachingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenEnabledSSLVerificationAndCachingCredentials {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @Nested
        class givenNoClientCertificate {

            @Test
            void whenNoBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, validBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, invalidBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }
        }

        @Nested
        class givenClientCertificate {

            @Test
            void whenNoBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, validBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, invalidBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }
        }

    }

    @Nested
    @DirtiesContext
    @TestPropertySource(
        properties = {
            "apiml.service.ssl.verifySslCertificatesOfServices=true"
        }
    )
    @SpringBootTest(
        classes = CachingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenEnabledSSLVerificationAndNoCachingCredentials {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @Nested
        class givenNoClientCertificate {

            @Test
            void whenNoBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, validBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, invalidBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }
        }

        @Nested
        class givenClientCertificate {

            @Test
            void whenNoBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, validBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(serviceIdHeader, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, invalidBasicAuth))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }
        }

    }

    @Nested
    class BasicAuthenticationManager {

        @Nested
        class MissingCredentials {

            @ParameterizedTest
            @CsvSource({
                ",,",
                "user,,",
                ",password,"
            })
            void givenNoCompleteCredentials_whenAuthorize_thenThrowException(String user, String password) {
                var authenticationManager = new SpringSecurityConfig.BasicAuthenticationManager(user, password == null ? null : password.toCharArray());
                StepVerifier.create(authenticationManager.authenticate(VALID_AUTHENTICATION))
                    .expectError(BadCredentialsException.class)
                    .verify();
            }

        }

        @Nested
        class ValidCredentials {

            private ReactiveAuthenticationManager basicAuthenticationManager = new SpringSecurityConfig.BasicAuthenticationManager(USER, PASSWORD.toCharArray());

            @Test
            void givenValidCredentials_whenAuthenticate_thenSuccess() {
                StepVerifier.create(basicAuthenticationManager.authenticate(VALID_AUTHENTICATION))
                    .assertNext(authentication -> {
                        assertNotSame(VALID_AUTHENTICATION, authentication);
                        assertTrue(authentication.isAuthenticated());
                        assertEquals(USER, authentication.getName());
                        String credentials = authentication.getCredentials() instanceof char[] chars ? new String(chars) : String.valueOf(authentication.getCredentials());
                        assertEquals(PASSWORD, credentials);
                    })
                    .verifyComplete();
            }

            @Test
            void givenValidCredentialsInAnotherForm_whenAuthenticate_thenSuccess() {
                var user = new Object() {
                    @Override
                    public String toString() {
                        return USER;
                    }
                };
                Authentication validAuthentication = new UsernamePasswordAuthenticationToken(user, PASSWORD, Collections.singleton(new SimpleGrantedAuthority("CACHING_SERVICE")));
                StepVerifier.create(basicAuthenticationManager.authenticate(validAuthentication))
                    .assertNext(authentication -> {
                        assertNotSame(validAuthentication, authentication);
                        assertTrue(authentication.isAuthenticated());
                        assertEquals(USER, authentication.getName());
                        assertTrue(authentication.getCredentials() instanceof char[]);
                        assertEquals(PASSWORD, new String((char[]) authentication.getCredentials()));
                    })
                    .verifyComplete();
            }

            @ParameterizedTest
            @CsvSource({
                ",,",
                "user,,",
                ",password,",
                "attacker,attempt"
            })
            void givenInvalidCredentials_whenAuthenticate_thenThrowException(String user, String password) {
                var authentication = new UsernamePasswordAuthenticationToken(user, password == null ? null : password.toCharArray(), Collections.singleton(new SimpleGrantedAuthority("CACHING_SERVICE")));
                StepVerifier.create(basicAuthenticationManager.authenticate(authentication))
                    .expectError(BadCredentialsException.class)
                    .verify();
            }

        }

    }

}
