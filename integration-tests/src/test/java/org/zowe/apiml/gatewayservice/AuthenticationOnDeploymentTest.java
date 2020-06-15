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
import lombok.SneakyThrows;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.util.categories.Flaky;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.service.DiscoveryUtils;
import org.zowe.apiml.util.service.RequestVerifier;
import org.zowe.apiml.util.service.VirtualService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
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
        final String serviceId = "multipleAuthentication".toLowerCase();

        try (
            final VirtualService service1 = new VirtualService(serviceId, 5678);
            final VirtualService service2 = new VirtualService(serviceId, 5679)
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
                        assertNotNull( x.getHeader(HttpHeaders.AUTHORIZATION));
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
                    assertNotNull( x.getHeader(HttpHeaders.AUTHORIZATION));
                    assertEquals("/verify/test", x.getRequestURI());
                    return true;
                });
            });
        }
    }

    /**
     * The idea of this test is check the case when Gateway uses retry call and if each call's headers are correct.
     * Scheme PassTicket removes cookies and i.e. ByPass should leave cookie in the call.
     *
     * Test start two instances of a service with different authentication scheme (PassTicket and ByPass). Gateway will
     * try call first service (with PassTicket). For the call cookie should be removed. This call will failed and
     * the Gateway will make retry to call second instance. It uses ByPass, so cookie should be in the headers. Test
     * checks if first call haven't changed request to make correct second call.
     */
    @Test
    @SneakyThrows
    void givenJwtInCookie_whenPassTicketFailed_thenSecondServiceWithByPassIsCalledCorrectly() {
        final String jwt = gatewayToken();
        final String serviceId = "fromPassTicketToByPassService".toLowerCase();

        try (
            final VirtualService service1 = new VirtualService(serviceId, 5678);
            final VirtualService service2 = new VirtualService(serviceId, 5679)
        ) {
            // service1 uses PassTickets, but it is stopped. Gateway retries call should be on service2
            service1.zombie();

            /*
             service2 uses ByPass scheme, it check if previous call didn't remove a cookie (required for service2, not
             for service1)
             */
            service2
                .setAuthentication(new Authentication(AuthenticationScheme.BYPASS, null))
                .addServlet("error", "/test/*", new HttpServlet() {

                    private static final long serialVersionUID = -8891890250560560219L;

                    @Override
                    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                        // verify transformation for ByPass scheme
                        assertNull(req.getHeader(HttpHeaders.AUTHORIZATION));
                        assertNotNull(req.getCookies());
                        assertEquals(1, req.getCookies().length);
                        Cookie cookie = req.getCookies()[0];
                        assertEquals(GATEWAY_TOKEN_COOKIE_NAME, cookie.getName());
                        assertEquals(jwt, cookie.getValue());

                        resp.setStatus(SC_NO_CONTENT);
                    }
                })
                .start()
                .waitForGatewayRegistration(2, TIMEOUT);

            given()
                .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
            .when()
                .get(DiscoveryUtils.getGatewayUrls().get(0) + "/api/v1/" + serviceId + "/test")
            .then()
                .statusCode(is(SC_NO_CONTENT));
        }
    }

}
