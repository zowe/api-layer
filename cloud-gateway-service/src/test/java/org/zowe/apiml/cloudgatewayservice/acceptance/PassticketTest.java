/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.netflix.appinfo.InstanceInfo;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.ticket.TicketResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AcceptanceTest
public class PassticketTest extends AcceptanceTestWithTwoServices {

    @MockBean
    InstanceInfoService instanceInfoService;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("apiml.service.gateway.proxy.enabled", () -> "false");
    }

    @BeforeEach
    void setup() {
        InstanceInfo info = InstanceInfo.Builder.newBuilder().setInstanceId("gateway").setHostName("localhost").setPort(getApplicationRegistry().findFreePort() + 1).setAppName("gateway").setStatus(InstanceInfo.InstanceStatus.UP).build();
        ServiceInstance instance = new EurekaServiceInstance(info);
        Mockito.when(instanceInfoService.getServiceInstance("gateway")).thenReturn(Mono.just(Collections.singletonList(instance)));
    }

    @Nested
    class GivenValidAuthentication {
        @Test
        void whenRequestingPassticketForAllowedAPPLID_thenTranslate() throws IOException {
            AtomicBoolean result = new AtomicBoolean(false);
            mockServerWithSpecificHttpResponse(200, "/serviceid2/test", 0, (headers) -> {
                    result.set(headers != null && headers.get(HttpHeaders.AUTHORIZATION) != null);
                }, "".getBytes()
            );
            TicketResponse response = new TicketResponse();
            response.setToken("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNjcxNDYxNjIzLCJleHAiOjE2NzE0OTA0MjMsImlzcyI6IkFQSU1MIiwianRpIjoiYmFlMTkyZTYtYTYxMi00MThhLWI2ZGMtN2I0NWI5NzM4ODI3IiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIifQ.Vt5UjJUlbmuzmmEIodAACtj_AOxlsWqkFrFyWh4_MQRRPCj_zMIwnzpqRN-NJvKtUg1zxOCzXv2ypYNsglrXc7cH9wU3leK1gjYxK7IJjn2SBEb0dUL5m7-h4tFq2zNhcGH2GOmTpE2gTQGSTvDIdja-TIj_lAvUtbkiorm1RqrNu2MGC0WfgOGiak3tj2tNJLv_Y1ZMxNjzyHgXBMuNPozQrd4Vtnew3x4yy85LrTYF7jJM3U-e3AD2yImftxwycQvbkjNb-lWadejTVH0MgHMr04wVdDd8Nq5q7yrZf7YPzhias8ehNbew5CHiKut9SseZ1sO2WwgfhpEfsN4okg");
            response.setUserId("user");
            response.setApplicationName("IZUDFLT");
            response.setTicket("ZOWE_DUMMY_PASS_TICKET");
            ObjectWriter writer = new ObjectMapper().writer();
            mockServerWithSpecificHttpResponse(200, "/gateway/api/v1/auth/ticket", getApplicationRegistry().findFreePort() + 1, (headers) -> {
                }, writer.writeValueAsString(response).getBytes()
            );
            given()
                .when()
                .get(basePath + "/serviceid2/api/v1/test")
                .then().statusCode(Matchers.is(SC_OK));
            assertTrue(result.get());
        }
    }
}
