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

import com.sun.net.httpserver.Headers;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.EurekaDashboardController;
import org.zowe.apiml.gateway.MockService;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@AcceptanceTest
public class EurekaEndpointsTests extends AcceptanceTestWithMockServices {

    private static final String USER_ID = "user";

    @MockitoBean
    EurekaDashboardController eurekaDashboardController;

    @Test
    void testEurekaHomePage() throws TemplateException, IOException {
        var headers = new Headers();
        headers.add("Set-Cookie", "jwtToken=jwt");
        headers.add("Set-Cookie", "LtpaToken2=ltpatoken");

        when(eurekaDashboardController.status(any(), any())).thenReturn(Mono.just("status page"));

        mockService("zosmf").scope(MockService.Scope.TEST)
            .addEndpoint("/zosmf/info")
                .responseCode(200)
                .contentType("application/json")
                .headers(headers)
                .bodyJson("{\"zosmf_version\":\"29\",\"zosmf_saf_realm\":\"SAFRealm\",\"zosmf_full_version\":\"29.0\"}")
        .and()
            .start();

        given()
        .when()
            .auth().preemptive().basic(USER_ID, "user")
            .get(URI.create("https://localhost:10011"))
        .then()
            .statusCode(200);
    }

}
