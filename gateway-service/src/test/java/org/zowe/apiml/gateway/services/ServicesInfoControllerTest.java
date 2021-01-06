/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.services;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.gateway.services.ServicesInfoController.SERVICES_URL;

@ExtendWith(MockitoExtension.class)
class ServicesInfoControllerTest {

    private static final String SERVICE_ID = "service";

    @Mock
    private ServicesInfoService servicesInfoService;

    @Test
    void whenServiceIsUp_thenReturnOK() {
        when(servicesInfoService.getServiceInfo(SERVICE_ID)).thenReturn(
                ServiceInfo.builder()
                        .serviceId(SERVICE_ID)
                        .status(InstanceInfo.InstanceStatus.UP).
                        build()
        );

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(SERVICES_URL + "/" + SERVICE_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", is(InstanceInfo.InstanceStatus.UP.toString()))
                .body("serviceId", is(SERVICE_ID));
        //@formatter:on
    }

    @Test
    void whenServiceDoesNotExist_thenReturnNotFound() {
        when(servicesInfoService.getServiceInfo(SERVICE_ID)).thenReturn(
                ServiceInfo.builder()
                        .serviceId(SERVICE_ID)
                        .status(InstanceInfo.InstanceStatus.UNKNOWN).
                        build()
        );

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(SERVICES_URL + "/" + SERVICE_ID)
       .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("status", is(InstanceInfo.InstanceStatus.UNKNOWN.toString()))
                .body("serviceId", is(SERVICE_ID));
        //@formatter:on
    }

}
