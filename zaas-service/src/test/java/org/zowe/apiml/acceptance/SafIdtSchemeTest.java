/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.restassured.http.Cookie;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.zaas.security.service.saf.SafRestAuthenticationService;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.io.IOException;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.ApimlConstants.AUTH_FAIL_HEADER;

/**
 * This test verifies that the token/client certificate was exchanged. The input is a valid apimlJwtToken/client certificate.
 * The output to be tested is the saf idt token.
 */
@AcceptanceTest
@TestPropertySource(properties = {
    "spring.profiles.active=debug",
    "apiml.security.x509.enabled=true",
    "apiml.security.x509.externalMapperUrl="
})
class SafIdtSchemeTest extends AcceptanceTestWithTwoServices {
    @Value("${server.ssl.keyStorePassword:password}")
    private char[] keystorePassword;
    @Value("${server.ssl.keyStore}")
    private String keystore;
    private final String clientKeystore = "../keystore/client_cert/client-certs.p12";

    @Autowired
    protected SafRestAuthenticationService safRestAuthenticationService;

    private RestTemplate mockTemplate;

    @BeforeEach
    void prepareTemplate() {
        mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(safRestAuthenticationService, "restTemplate", mockTemplate);
    }

    @Nested
    class GivenValidJwtToken {
        Cookie validJwtToken;

        @BeforeEach
        void setUp() {
            validJwtToken = securityRequests.validJwtToken();
        }

        @Nested
        class WhenSafIdtRequestedByService {
            @BeforeEach
            void prepareService() throws IOException {
                applicationRegistry.clearApplications();
                MetadataBuilder customBuilder = MetadataBuilder.customInstance();
                customBuilder.withSafIdt();
                MetadataBuilder defaultBuilder = MetadataBuilder.defaultInstance();
                defaultBuilder.withSafIdt();

                applicationRegistry.addApplication(serviceWithDefaultConfiguration, defaultBuilder, false);
                applicationRegistry.addApplication(serviceWithCustomConfiguration, customBuilder, true);
                applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());

                reset(mockClient);
                mockValid200HttpResponse();
            }

            // Valid token is provided within the headers.
            @Test
            void thenValidTokenIsProvided() throws IOException {
                String resultSafToken = Jwts.builder()
                    .setExpiration(DateUtils.addMinutes(new Date(), 10))
                    .signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256))
                    .compact();

                ResponseEntity<SafRestAuthenticationService.Token> response = mock(ResponseEntity.class);
                when(mockTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(SafRestAuthenticationService.Token.class)))
                    .thenReturn(response);
                SafRestAuthenticationService.Token responseBody =
                    new SafRestAuthenticationService.Token(resultSafToken, "applid");
                when(response.getBody()).thenReturn(responseBody);

                given()
                    .cookie(validJwtToken)
                    .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                    .then()
                    .statusCode(is(HttpStatus.SC_OK));

                ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
                verify(mockClient, times(1)).execute(captor.capture());

                assertHeaderWithValue(captor.getValue(), "X-SAF-Token", resultSafToken);
            }
        }
    }

    @Nested
    class ThenNoTokenIsProvided {
        @Nested
        class WhenSafIdtRequestedByService {
            @BeforeEach
            void prepareService() throws IOException {
                MetadataBuilder metadataBuilder = MetadataBuilder.defaultInstance().withSafIdt();
                applicationRegistry.addApplication(serviceWithDefaultConfiguration, metadataBuilder, false);
                applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());

                reset(mockClient);
                mockValid200HttpResponse();
            }

            @Test
            void givenInvalidJwtToken() throws IOException {
                Cookie withInvalidToken = new Cookie.Builder("apimlAuthenticationToken=invalidValue").build();

                //@formatter:off
                given()
                    .cookie(withInvalidToken)
                    .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                    .then()
                    .statusCode(is(HttpStatus.SC_OK));
                //@formatter:on

                ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
                verify(mockClient, times(1)).execute(captor.capture());

                verify(mockTemplate, times(0))
                    .exchange(any(), eq(HttpMethod.POST), any(), eq(SafRestAuthenticationService.Token.class));

                Header zoweAuthFailureHeader = captor.getValue().getFirstHeader(AUTH_FAIL_HEADER);
                assertNotNull(zoweAuthFailureHeader);
                assertEquals("ZWEAG102E Token is not valid", zoweAuthFailureHeader.getValue());
            }
        }
    }

    @Nested
    class GivenClientCertificate {
        String resultSafToken;

        @BeforeEach
        void setUp() throws Exception {
            SslContextConfigurer configurer = new SslContextConfigurer(keystorePassword, clientKeystore, keystore);
            SslContext.prepareSslAuthentication(configurer);

            applicationRegistry.clearApplications();
            MetadataBuilder defaultBuilder = MetadataBuilder.defaultInstance();
            defaultBuilder.withSafIdt();
            applicationRegistry.addApplication(serviceWithDefaultConfiguration, defaultBuilder, false);
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());

            reset(mockClient);

            resultSafToken = Jwts.builder()
                .setExpiration(DateUtils.addMinutes(new Date(), 10))
                .signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256))
                .compact();

            ResponseEntity<SafRestAuthenticationService.Token> response = mock(ResponseEntity.class);
            when(mockTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(SafRestAuthenticationService.Token.class)))
                .thenReturn(response);
            SafRestAuthenticationService.Token responseBody =
                new SafRestAuthenticationService.Token(resultSafToken, "applid");
            when(response.getBody()).thenReturn(responseBody);

            mockValid200HttpResponse();
        }

        @Nested
        class WhenClientAuthCertificate {
            @Test
            void thenValidSafIdTokenProvided() throws IOException {
                given()
                    .config(SslContext.clientCertUser)
                    .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                    .then()
                    .statusCode(is(HttpStatus.SC_OK));

                ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
                verify(mockClient, times(1)).execute(captor.capture());
                assertHeaderWithValue(captor.getValue(), "X-SAF-Token", resultSafToken);
                assertThat(captor.getValue().getHeaders(AUTH_FAIL_HEADER).length, is(0));
            }
        }

        /**
         * When client certificate from request does not have extended key usage set correctly and can't be used for
         * client authentication then request will continue with X-Zowe-Auth-Failure header only.
         */
        @Nested
        class WhenNoClientAuthCertificate {
            @Test
            void thenNoSafIdTokenProvided() throws IOException {

                given()
                    .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                    .then()
                    .statusCode(is(HttpStatus.SC_OK));

                ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
                verify(mockClient, times(1)).execute(captor.capture());
                assertThat(captor.getValue().getHeaders("X-SAF-Token").length, is(0));
                assertHeaderWithValue(captor.getValue(), AUTH_FAIL_HEADER, "ZWEAG160E No authentication provided in the request");
            }
        }
    }

    private void assertHeaderWithValue(HttpUriRequest request, String header, String value) {
        assertThat(request.getHeaders(header).length, is(1));
        assertThat(request.getFirstHeader(header).getValue(), is(value));
    }
}
