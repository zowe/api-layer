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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.services.ServiceInfo;

import java.util.Arrays;
import java.util.Collections;

import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.gateway.services.ServicesInfoController.SERVICES_FULL_URL;
import static org.zowe.apiml.gateway.services.ServicesInfoController.SERVICES_SHORT_URL;
import static org.zowe.apiml.gateway.services.ServicesInfoService.CURRENT_VERSION;
import static org.zowe.apiml.gateway.services.ServicesInfoService.VERSION_HEADER;

@ExtendWith(MockitoExtension.class)
class ServicesInfoControllerTest {

    private static final String SERVICE_ID = "service";

    private final ServiceInfo serviceInfo = ServiceInfo.builder()
            .serviceId(SERVICE_ID)
            .status(InstanceInfo.InstanceStatus.UP)
            .build();

    @Mock
    private ServicesInfoService servicesInfoService;

    @ParameterizedTest(name = "whenGetAllServices_thenReturnList: {0}")
    @ValueSource(strings = {SERVICES_SHORT_URL, SERVICES_FULL_URL})
    void whenGetAllServices_thenReturnList(String url) {
        when(servicesInfoService.getServicesInfo(null)).thenReturn(
                Arrays.asList(serviceInfo, serviceInfo)
        );

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(url)
        .then()
                .statusCode(HttpStatus.OK.value())
                .header(VERSION_HEADER, CURRENT_VERSION)
                .body("size()", is(2));
        //@formatter:on
    }

    @ParameterizedTest(name = "whenFilterByApiId_thenReturnList: {0}")
    @ValueSource(strings = {SERVICES_SHORT_URL, SERVICES_FULL_URL})
    void whenFilterByApiId_thenReturnList(String url) {
        String apiId = "apiId";
        when(servicesInfoService.getServicesInfo(apiId)).thenReturn(
                Arrays.asList(serviceInfo, serviceInfo)
        );

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(String.format("%s?apiId=%s", url, apiId))
        .then()
                .statusCode(HttpStatus.OK.value())
                .header(VERSION_HEADER, CURRENT_VERSION)
                .body("size()", is(2));
        //@formatter:on
    }

    @ParameterizedTest(name = "whenApiIdDoesNotExists_thenReturn404: {0}")
    @ValueSource(strings = {SERVICES_SHORT_URL, SERVICES_FULL_URL})
    void whenApiIdDoesNotExists_thenReturn404(String url) {
        String apiId = "apiId";
        when(servicesInfoService.getServicesInfo(apiId)).thenReturn(Collections.emptyList());

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(String.format("%s?apiId=%s", url, apiId))
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
        //@formatter:on
    }

    @ParameterizedTest(name = "whenServiceIsUp_thenReturnOK: {0}")
    @ValueSource(strings = {SERVICES_SHORT_URL, SERVICES_FULL_URL})
    void whenServiceIsUp_thenReturnOK(String url) {
        when(servicesInfoService.getServiceInfo(SERVICE_ID)).thenReturn(serviceInfo);

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(url + "/" + SERVICE_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .header(VERSION_HEADER, CURRENT_VERSION)
                .body("status", is(InstanceInfo.InstanceStatus.UP.toString()))
                .body("serviceId", is(SERVICE_ID));
        //@formatter:on
    }

    @ParameterizedTest(name = "whenServiceDoesNotExist_thenReturnNotFound: {0}")
    @ValueSource(strings = {SERVICES_SHORT_URL, SERVICES_FULL_URL})
    void whenServiceDoesNotExist_thenReturnNotFound(String url) {
        serviceInfo.setStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        when(servicesInfoService.getServiceInfo(SERVICE_ID)).thenReturn(serviceInfo);

        //@formatter:off
        given()
                .standaloneSetup(new ServicesInfoController(servicesInfoService))
        .when()
                .get(url + "/" + SERVICE_ID)
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("status", is(InstanceInfo.InstanceStatus.UNKNOWN.toString()))
                .body("serviceId", is(SERVICE_ID));
        //@formatter:on
    }

}
