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
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.security.common.login.LoginRequest;

import java.io.IOException;
import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.ApimlConstants.COOKIE_AUTH_NAME;

/**
 * Verify that the behavior configured for the routing chooses for the same user the same service instance.
 */
@AcceptanceTest
public class DeterministicUserBasedRouting extends AcceptanceTestWithTwoServices {
    @BeforeEach
    public void prepareApplications() {
        applicationRegistry.clearApplications();
        applicationRegistry.addApplication(serviceWithDefaultConfiguration, false, true, false, "authentication");
        applicationRegistry.addApplication(serviceWithCustomConfiguration, true, false, true, "authentication");
    }

    @Nested
    class GivenAuthenticatedUserAndMoreInstancesOfService {
        @Nested
        class WhenCallingToServiceMultipleTimes {
            @Test
            void thenCallTheSameInstance() throws IOException {
                LoginRequest loginRequest = new LoginRequest("user", "user");

                Cookie token = given()
                    .contentType(JSON)
                    .body(loginRequest)
                .when()
                    .post(basePath + "/gateway/api/v1/auth/login")
                .then()
                    .statusCode(is(SC_NO_CONTENT))
                    .extract()
                    .detailedCookie(COOKIE_AUTH_NAME);

                applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());

                URI selectedInFirstCall = routeToService(token);
                URI selectedInSecondCall = routeToService(token);
                assertThat(selectedInFirstCall.compareTo(selectedInSecondCall), is(0));
            }
        }

        private URI routeToService(Cookie token) throws IOException {
            reset(mockClient);
            mockValid200HttpResponse();

            // First call stores the information about selected service
            given()
                .cookie(token)
                .when()
                .get(basePath + serviceWithCustomConfiguration.getPath())
                .then()
                .statusCode(is(SC_OK));

            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(mockClient, times(1)).execute(captor.capture());

            HttpUriRequest toVerify = captor.getValue();
            return toVerify.getURI();
        }
    }
}
