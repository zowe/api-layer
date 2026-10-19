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

import freemarker.template.TemplateException;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.EurekaDashboardController;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@AcceptanceTest
public class EurekaEndpointsTests extends AcceptanceTestWithMockServices {

    private static final String USER_ID = "user";
    private static final String DISCOVERY_BASE_PATH = "https://localhost:" + DISCOVERY_PORT;

    @MockitoBean
    EurekaDashboardController eurekaDashboardController;

    @Test
    void givenDiscoveryPort_whenGetRoot_thenForwardToEurekaHomePage() throws TemplateException, IOException {
        when(eurekaDashboardController.status(any(), any())).thenReturn(Mono.just("status page"));
        mockZosmfSuccess();

        given()
            .redirects().follow(false)
        .when()
            .auth().preemptive().basic(USER_ID, "user")
            .get(DISCOVERY_BASE_PATH + "/")
        .then()
            .statusCode(200)
            .body(equalTo("status page"));
    }

    @Test
    void givenGatewayPort_whenGetRoot_thenReturnGatewayHomePage() {
        given()
            .redirects().follow(false)
        .when()
            .get(basePath + "/")
        .then()
            .statusCode(200)
            .contentType(MediaType.TEXT_HTML_VALUE)
            .body(containsString("<title>API Mediation Layer</title>"));

        verifyNoInteractions(eurekaDashboardController);
    }

    @Test
    void givenGatewayPort_whenGetEurekaApi_thenReturnNotFound() {
        given()
        .when()
            .get(basePath + "/eureka/apps")
        .then()
            .statusCode(404)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("messages[0].messageKey", equalTo("org.zowe.apiml.common.notFound"));
    }

    @Test
    void givenDiscoveryPort_whenGetEurekaApiWithClientCertificate_thenReturnApplications() {
        given()
            .config(RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(
                new SSLSocketFactory(httpConfig.secureSslContext(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
            )))
            .accept(MediaType.APPLICATION_JSON_VALUE)
        .when()
            .get(DISCOVERY_BASE_PATH + "/eureka/apps")
        .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("applications", notNullValue());
    }

    @Test
    void testEurekaPeerNodeReplica() {
        given()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                """
                    {"replicationList":[{"appName":"GATEWAY","id":"apiml-2:gateway:10010","lastDirtyTimestamp":1751961556383,"status":"UP","instanceInfo":{"instanceId":"apiml-2:gateway:10010","hostName":"apiml-2","app":"GATEWAY","ipAddr":"127.0.0.1","status":"UP","overriddenStatus":"UNKNOWN","port":{"$":10010,"@enabled":"false"},"securePort":{"$":10010,"@enabled":"true"},"countryId":1,"dataCenterInfo":{"@class":"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo","name":"MyOwn"}
                """
            )
        .when()
            .post(DISCOVERY_BASE_PATH + "/eureka/peerreplication/batch/")
        .then()
            .statusCode(403);
    }

}
