/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.services;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.services.ServiceInfo;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.zowe.apiml.cloudgatewayservice.services.ServicesInfoController.SERVICES_URL;
import static org.zowe.apiml.cloudgatewayservice.services.ServicesInfoService.CURRENT_VERSION;
import static org.zowe.apiml.cloudgatewayservice.services.ServicesInfoService.VERSION_HEADER;

@ExtendWith(MockitoExtension.class)
class ServicesInfoControllerTest {

    private static final String SERVICE_ID = "service";

    private final ServiceInfo serviceInfo = ServiceInfo.builder()
            .serviceId(SERVICE_ID)
            .status(InstanceInfo.InstanceStatus.UP)
            .build();

    @Mock
    private ServicesInfoService servicesInfoService;

    @Test
    void whenGetAllServices_thenReturnList() {
        when(servicesInfoService.getServicesInfo(null)).thenReturn(
                Arrays.asList(serviceInfo, serviceInfo)
        );

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(SERVICES_URL)
        .then()
                .statusCode(HttpStatus.OK.value())
                .header(VERSION_HEADER, CURRENT_VERSION)
                .body("size()", is(2));
        //@formatter:on
    }

    @Test
    void whenFilterByApiId_thenReturnList() {
        String apiId = "apiId";
        when(servicesInfoService.getServicesInfo(apiId)).thenReturn(
                Arrays.asList(serviceInfo, serviceInfo)
        );

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(String.format("%s?apiId=%s",SERVICES_URL, apiId))
        .then()
                .statusCode(HttpStatus.OK.value())
                .header(VERSION_HEADER, CURRENT_VERSION)
                .body("size()", is(2));
        //@formatter:on
    }

    @Test
    void whenApiIdDoesNotExists_thenReturn404() {
        String apiId = "apiId";
        when(servicesInfoService.getServicesInfo(apiId)).thenReturn(Collections.emptyList());

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(String.format("%s?apiId=%s",SERVICES_URL, apiId))
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
        //@formatter:on
    }

    @Test
    void whenServiceIsUp_thenReturnOK() {
        when(servicesInfoService.getServiceInfo(SERVICE_ID)).thenReturn(serviceInfo);

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(SERVICES_URL + "/" + SERVICE_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .header(VERSION_HEADER, CURRENT_VERSION)
                .body("status", is(InstanceInfo.InstanceStatus.UP.toString()))
                .body("serviceId", is(SERVICE_ID));
        //@formatter:on
    }

    @Test
    void whenServiceDoesNotExist_thenReturnNotFound() {
        serviceInfo.setStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        when(servicesInfoService.getServiceInfo(SERVICE_ID)).thenReturn(serviceInfo);

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
