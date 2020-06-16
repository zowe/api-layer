/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice;

import io.restassured.RestAssured;
import org.apache.catalina.LifecycleException;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.util.categories.Flaky;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.service.RequestVerifier;
import org.zowe.apiml.util.service.VirtualService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.zowe.apiml.gatewayservice.SecurityUtils.*;

/**
 * This test requires to allow endpoint routes on gateway (ie profile dev)
 * <p>
 * Instance settings
 * - gateway-service.yml
 * - apiml.security.auth.provider = dummy
 * - environment-configuration.yml
 * - credentials.user = user
 * - credentials.password = user
 */
@TestsNotMeantForZowe
public class AuthenticationOnDeploymentTest {

    private static final int TIMEOUT = 10;

    private RequestVerifier verifier;

    @BeforeEach
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        verifier = RequestVerifier.getInstance();
        verifier.clear();
    }

    @Test
    @Flaky
    public void testMultipleAuthenticationSchemes() throws Exception {
        final String jwt = gatewayToken();

        try (
            final VirtualService service1 = new VirtualService("testService", 5678);
            final VirtualService service2 = new VirtualService("testService", 5679)
        ) {
            // start first instance - without passTickets
            service1
                .addVerifyServlet()
                .start()
                .waitForGatewayRegistration(1, TIMEOUT);


            // on each gateway make a call to service
            service1.getGatewayVerifyUrls().forEach(x ->
                given()
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
                    .when().get(x + "/test")
                    .then().statusCode(is(SC_OK))
            );

            // verify if each gateway sent request to service
            service1.getGatewayVerifyUrls().forEach(gw ->
                verifier.existAndClean(service1, x -> x.getHeader(HttpHeaders.AUTHORIZATION) == null && x.getRequestURI().equals("/verify/test"))
            );

            // start second service (with passTicket authorization)
            service2
                .addVerifyServlet()
                .setAuthentication(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "ZOWEAPPL"))
                .start()
                .waitForGatewayRegistration(2, TIMEOUT);

            // on each gateway make calls (count same as instances) to service
            service1.getGatewayVerifyUrls().forEach(x -> {
                given()
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
                    .when().get(x + "/test")
                    .then().statusCode(is(SC_OK));
            });
            service2.getGatewayVerifyUrls().forEach(x -> {
                given()
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
                    .when().get(x + "/test")
                    .then().statusCode(is(SC_OK));
            });

            // verify if each gateway sent request to service (one with and one without passTicket)
            service1.getGatewayVerifyUrls().forEach(gw -> {
                    verifier.existAndClean(service1, x -> x.getHeader(HttpHeaders.AUTHORIZATION) == null && x.getRequestURI().equals("/verify/test"));
                    verifier.existAndClean(service2, x -> {
                        assertNotNull(x.getHeader(HttpHeaders.AUTHORIZATION));
                        assertEquals("/verify/test", x.getRequestURI());
                        return true;
                    });
                }
            );

            // stop first service without authentication
            service1
                .unregister()
                .waitForGatewayUnregistering(2, TIMEOUT)
                .stop();

            // check second service, all called second one with passTicket, same url like service1 (removed)
            service1.getGatewayVerifyUrls().forEach(x -> {
                given()
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
                    .when().get(x + "/test")
                    .then().statusCode(is(SC_OK));
            });
            service1.getGatewayVerifyUrls().forEach(gw -> {
                verifier.existAndClean(service2, x -> {
                    assertNotNull(x.getHeader(HttpHeaders.AUTHORIZATION));
                    assertEquals("/verify/test", x.getRequestURI());
                    return true;
                });
            });
        }
    }

    @Test
    @Flaky
    void testReregistration() throws Exception {

        try (
            final VirtualService service1 = new VirtualService("testService1", 5678);
            final VirtualService service2 = new VirtualService("testService1", 5679);
            final VirtualService service4 = new VirtualService("testService1", 5678)
        ) {
            List<VirtualService> serviceList = Arrays.asList(service1, service2);

            serviceList.forEach(s -> {
                try {
                    s.addVerifyServlet().start().waitForGatewayRegistration(1, TIMEOUT);
                } catch (IOException | LifecycleException e) {
                    e.printStackTrace();
                }
            });

            serviceList.forEach(s -> {
                try {
                    s.unregister().waitForGatewayUnregistering(1, TIMEOUT).stop();
                } catch (LifecycleException e) {
                    e.printStackTrace();
                }
            });
//            register service with the same name
            service4.addVerifyServlet().start().waitForGatewayRegistration(1, TIMEOUT);
            // on each gateway make a call to service
            service4.getGatewayVerifyUrls().forEach(x ->
                given()
                    .when().get(x + "/test")
                    .then().statusCode(is(SC_OK))
            );

            // verify if each gateway sent request to service
            service4.getGatewayVerifyUrls().forEach(gw ->
                verifier.existAndClean(service4, x -> x.getHeader(HttpHeaders.AUTHORIZATION) == null && x.getRequestURI().equals("/verify/test"))
            );
            service4.unregister().waitForGatewayUnregistering(1, TIMEOUT).stop();

        }
    }
}
