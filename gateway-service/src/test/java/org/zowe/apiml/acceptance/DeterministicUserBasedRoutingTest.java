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
import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;

import java.io.IOException;
import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

/**
 * Verify that the behavior configured for the routing chooses for the same user the same service instance.
 */
@AcceptanceTest
class DeterministicUserBasedRoutingTest extends AcceptanceTestWithTwoServices {
    @Autowired
    protected LoadBalancerCache cache;

    public void prepareApplications() {
        cache.getLocalCache().clear();
        applicationRegistry.clearApplications();
        applicationRegistry.addApplication(serviceWithDefaultConfiguration, false, true, false, "authentication");
        applicationRegistry.addApplication(serviceWithCustomConfiguration, true, false, true, "authentication");
    }

    @Nested
    class GivenAuthenticatedUserAndMoreInstancesOfService {
        @Nested
        class WhenCallingToServiceMultipleTimes {

            boolean initialized = false;

            @RepeatedTest(10)
            void thenCallTheSameInstance(RepetitionInfo repetitionInfo) throws IOException {

                // initialize the cache and registry only once on first repetition
                if (repetitionInfo.getCurrentRepetition() == 1) {
                    prepareApplications();
                }

                Cookie token = securityRequests.validJwtToken();

                applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());

                URI selectedInFirstCall = routeToService(token, SC_OK);
                URI selectedInSecondCall = routeToService(token, SC_OK);
                URI selectedInThirdCall = routeToService(token, SC_OK);

                assertThat(selectedInFirstCall.compareTo(selectedInSecondCall), is(0));
                assertThat(selectedInFirstCall.compareTo(selectedInThirdCall), is(0));
            }
        }

        private URI routeToService(Cookie token, int status) throws IOException {
            reset(mockClient);
            mockResponseWithStatus(status);

            // First call stores the information about selected service
            given()
                .cookie(token)
                .when()
                .get(basePath + serviceWithCustomConfiguration.getPath())
                .then()
                .statusCode(is(status));

            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(mockClient, times(1)).execute(captor.capture());

            HttpUriRequest toVerify = captor.getValue();
            return toVerify.getURI();
        }

        private void mockResponseWithStatus(int status) throws IOException {
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            Mockito.when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), status, ""));
            Mockito.when(response.getAllHeaders()).thenReturn(new Header[]{});
            Mockito.when(mockClient.execute(any())).thenReturn(response);
        }
    }
}
