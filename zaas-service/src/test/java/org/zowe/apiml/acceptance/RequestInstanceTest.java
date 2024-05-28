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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.zaas.ribbon.loadbalancer.LoadBalancerConstants;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.core.Is.is;

@AcceptanceTest
public class RequestInstanceTest extends AcceptanceTestWithTwoServices {

    @BeforeEach
    public void prepareApplications() {
        applicationRegistry.clearApplications();
        applicationRegistry.addApplication(serviceWithDefaultConfiguration, MetadataBuilder.defaultInstance(),false);
        applicationRegistry.addApplication(serviceWithCustomConfiguration, MetadataBuilder.customInstance(),true);
    }

    @Nested
    class WhenValidInstanceId {

        @ParameterizedTest
        @ValueSource(strings = {"serviceid1", "serviceid1-copy"})
        void chooseCorrectService(String instanceId) throws IOException {
            mockValid200HttpResponse();
            applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
            given().header(LoadBalancerConstants.INSTANCE_HEADER_KEY, instanceId).
                when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_OK)).header("X-InstanceId", is(instanceId));
        }
    }

    @Nested
    class WhenNonExistingInstanceId {
        @Test
        void cantChooseServer() throws IOException {
            mockValid200HttpResponse();
            applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
            given().header(LoadBalancerConstants.INSTANCE_HEADER_KEY, "non-existing").
                when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then()
                .statusCode(is(SC_SERVICE_UNAVAILABLE));
        }
    }
}
