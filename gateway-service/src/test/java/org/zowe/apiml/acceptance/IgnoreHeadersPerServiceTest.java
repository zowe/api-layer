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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;

import java.io.IOException;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.mockito.Mockito.*;

@AcceptanceTest
public class IgnoreHeadersPerServiceTest extends AcceptanceTestWithTwoServices {
    private static final String HEADER = "myheader";
    private static final String HEADER_VALUE = "myheadervalue";

    @BeforeEach
    void setup() throws IOException {
        applicationRegistry.clearApplications();
        reset(mockClient);

        mockValid200HttpResponse();

        MetadataBuilder customBuilder = MetadataBuilder.customInstance()
            .withIgnoredHeaders(Collections.singletonList(HEADER));

        applicationRegistry.addApplication(serviceWithDefaultConfiguration, MetadataBuilder.defaultInstance(), false);
        applicationRegistry.addApplication(serviceWithCustomConfiguration, customBuilder, false);
    }

    @Nested
    class GivenServiceWithIgnoredHeaders {
        @BeforeEach
        void setApplication() {
            applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
            discoveryClient.createRefreshCacheEvent();
        }

        @Test
        void whenIncludeIgnoredHeader_thenThatHeaderIsIgnored() throws IOException {
            given()
                .header(HEADER, HEADER_VALUE)
                .when()
                .get(basePath + serviceWithCustomConfiguration.getPath())
                .then()
                .statusCode(HttpStatus.SC_OK);

            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(mockClient, times(1)).execute(captor.capture());

            HttpUriRequest toVerify = captor.getValue();
            org.apache.http.Header[] headers = toVerify.getHeaders(HEADER);
            assertThat(headers, arrayWithSize(0));
        }

        @Test
        void whenIncludeUnignoredHeader_thenThatHeaderIsIncluded() throws IOException {
            String includedHeader = HEADER + "unignored";
            given()
                .header(includedHeader, HEADER_VALUE)
                .when()
                .get(basePath + serviceWithCustomConfiguration.getPath())
                .then()
                .statusCode(HttpStatus.SC_OK);

            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(mockClient, times(1)).execute(captor.capture());

            HttpUriRequest toVerify = captor.getValue();
            org.apache.http.Header[] headers = toVerify.getHeaders(includedHeader);
            assertThat(headers, arrayWithSize(1));
        }
    }

    @Nested
    class GivenServiceWithNoIgnoredHeaders {
        @BeforeEach
        void setApplication() {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            discoveryClient.createRefreshCacheEvent();
        }

        @Test
        void whenIncludeHeaderIgnoredByOtherService_thenThatHeaderIsIncluded() throws IOException {
            given()
                .header(HEADER, HEADER_VALUE)
                .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then()
                .statusCode(HttpStatus.SC_OK);

            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(mockClient, times(1)).execute(captor.capture());

            HttpUriRequest toVerify = captor.getValue();
            org.apache.http.Header[] headers = toVerify.getHeaders(HEADER);
            assertThat(headers, arrayWithSize(1));
        }
    }
}
