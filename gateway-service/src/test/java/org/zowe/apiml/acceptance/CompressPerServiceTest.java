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

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;

@AcceptanceTest
class CompressPerServiceTest extends AcceptanceTestWithTwoServices {
    @Nested
    class GivenServiceAcceptsCompression {
        @Nested
        class ForAllPaths {
            @Test
            void thenResponseIsCompressed() throws IOException {
                mockValid200HttpResponse();
                applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
                discoveryClient.createRefreshCacheEvent();
                given()
                    .when()
                    .get(basePath + serviceWithCustomConfiguration.getPath())
                    .then()
                    .statusCode(is(SC_OK))
                    .header("Content-Encoding", is("gzip"));
            }
        }

        @Nested
        class ForSomePaths {
            @BeforeEach
            void prepareServices() {
                // Make sure customConfiguration service has custom metadata for this set.
                applicationRegistry.clearApplications();
                MetadataBuilder customBuilder = MetadataBuilder.customInstance()
                    .withCompressionPath("/**/test")
                    .withCompression(true);

                applicationRegistry.addApplication(serviceWithDefaultConfiguration, MetadataBuilder.defaultInstance(), false);
                applicationRegistry.addApplication(serviceWithCustomConfiguration, customBuilder, true);
                applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            }

            @Test
            void forValidPath_thenResponseIsCompressed() throws IOException {
                mockValid200HttpResponse();
                applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
                discoveryClient.createRefreshCacheEvent();
                given()
                    .when()
                    .get(basePath + serviceWithCustomConfiguration.getPath())
                    .then()
                    .statusCode(is(SC_OK))
                    .header("Content-Encoding", is("gzip"));
            }

            @Test
            void forInvalidPath_thenResponseIsNotCompressed() throws IOException {
                mockValid200HttpResponse();
                applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
                discoveryClient.createRefreshCacheEvent();
                given()
                    .when()
                    .get(basePath + serviceWithCustomConfiguration.getPath() + "/noncompressed")
                    .then()
                    .statusCode(is(SC_OK))
                    .header("Content-Encoding", Matchers.nullValue());
            }
        }
    }

    @Test
    void givenServiceDoesntAcceptsCompression_thenResponseIsNotCompressed() throws IOException {
        mockValid200HttpResponse();
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        discoveryClient.createRefreshCacheEvent();
        given()
            .when()
            .get(basePath + serviceWithCustomConfiguration.getPath())
            .then()
            .statusCode(is(SC_OK))
            .header("Content-Encoding", Matchers.nullValue());
    }
}
