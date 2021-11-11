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

import io.restassured.http.Cookie;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.gateway.security.service.saf.SafRestAuthenticationService;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

/**
 * This test verifies that the token was exchanged. The input is a valid apimlJwtToken. The output to be tested is
 * the saf idt token.
 */
@AcceptanceTest
class SafIdtSchemeTest extends AcceptanceTestWithTwoServices {
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
                String resultSafToken = "resultSafToken";

                ResponseEntity<Object> response = mock(ResponseEntity.class);
                when(mockTemplate.postForEntity(any(), any(), any())).thenReturn(response);
                when(response.getStatusCode()).thenReturn(org.springframework.http.HttpStatus.CREATED);
                SafRestAuthenticationService.Token responseBody = new SafRestAuthenticationService.Token();
                responseBody.setJwt(resultSafToken);
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
                applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());

                reset(mockClient);
                mockValid200HttpResponse();
            }

            @Test
            void givenInvalidJwtToken() {
                Cookie withInvalidToken = new Cookie.Builder("apimlAuthenticationToken=invalidValue").build();

                given()
                    .cookie(withInvalidToken)
                    .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                    .then()
                    .statusCode(is(HttpStatus.SC_OK));

                verify(mockTemplate, times(0)).postForEntity(any(), any(), any());
            }
        }
    }

    private void assertHeaderWithValue(HttpUriRequest request, String header, String value) {
        assertThat(request.getHeaders(header).length, is(1));
        assertThat(request.getFirstHeader(header).getValue(), is(value));
    }
}
