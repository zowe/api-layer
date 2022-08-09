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

import io.restassured.response.Response;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;

@AcceptanceTest
public class AddHeadersPerServiceTest extends AcceptanceTestWithTwoServices {
    private static final String HEADER = "my-header";
    private static final String VALUE = "my-value";

    @BeforeEach
    void setup() throws IOException {
        applicationRegistry.clearApplications();
        reset(mockClient);

        mockValid200HttpResponse();

        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER, VALUE);
        MetadataBuilder customBuilder = MetadataBuilder.customInstance()
            .withAddedHeaders(headers);

        applicationRegistry.addApplication(serviceWithDefaultConfiguration, MetadataBuilder.defaultInstance(), false);
        applicationRegistry.addApplication(serviceWithCustomConfiguration, customBuilder, false);
    }

    @Nested
    class GivenServiceWithAddedHeaders {
        @BeforeEach
        void setApplication() {
            applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
            discoveryClient.createRefreshCacheEvent();
        }

        @Test
        void whenIncludeAddedHeader_thenHeaderIsAdded() {
            given()
                .when()
                .get(basePath + serviceWithCustomConfiguration.getPath())
                .then()
                .statusCode(HttpStatus.SC_OK)
                .header(HEADER, VALUE);
        }

        @Test
        void whenIncludedHeaderAlreadyAddedByService_thenHeaderIsOverridden() throws IOException {
            mockValid200HttpResponseWithHeaders(new Header[]{
                new BasicHeader(HEADER, "other-value")
            });

            given()
                .when()
                .get(basePath + serviceWithCustomConfiguration.getPath())
                .then()
                .statusCode(HttpStatus.SC_OK)
                .header(HEADER, VALUE);
        }
    }

    @Nested
    class GivenServiceWithoutAddedHeaders {
        @BeforeEach
        void setApplication() {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            discoveryClient.createRefreshCacheEvent();
        }

        @Test
        void whenGetResponse_thenHeaderIsNotAdded() {
            Response response = given()
                .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath());

            response.then().statusCode(HttpStatus.SC_OK);
            assertThat(response.getHeader(HEADER)).isNull();
        }
    }
}
