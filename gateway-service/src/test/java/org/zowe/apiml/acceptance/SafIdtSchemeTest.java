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
import io.restassured.response.ExtractableResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

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
                applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());

                reset(mockClient);
                mockValid200HttpResponse();
            }

            // Valid token is provided within the headers.
            @Test
            void thenValidTokenIsProvided() throws IOException {
                given()
                    .cookie(validJwtToken)
                .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then()
                    .statusCode(is(HttpStatus.SC_OK));

                ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
                verify(mockClient, times(1)).execute(captor.capture());

                assertHeaderWithValue(captor.getValue(), "X-SAF-Token", "validToken" + validJwtToken.getValue());
            }
        }
    }

    private void assertHeaderWithValue(HttpUriRequest request, String header, String value) {
        assertThat(request.getHeaders(header).length, is(1));
        assertThat(request.getFirstHeader(header).getValue(), is(value));
    }
}
