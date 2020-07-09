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
import org.springframework.boot.actuate.health.Status;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.util.categories.Flaky;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.service.RequestVerifier;
import org.zowe.apiml.util.service.VirtualService;

import java.io.IOException;
import java.net.InetAddress;
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

    private static final int TIMEOUT = 3;

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
            final VirtualService service1 = new VirtualService("testService3", 5678);
            final VirtualService service2 = new VirtualService("testService3", 5679);
            final VirtualService service4 = new VirtualService("testService3", 5678)
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

    @Test
    @Flaky
    void testServiceStatus() throws Exception {

        String serviceId = "testservice4";
        String host = InetAddress.getLocalHost().getHostName();

        List<Integer> ports = Arrays.asList(5678, 5679, 5680);

        try (
            final VirtualService service1 = new VirtualService(serviceId, 5678);
            final VirtualService service2 = new VirtualService(serviceId, 5679);
            final VirtualService service3 = new VirtualService(serviceId, 5680);
            final VirtualService service4 = new VirtualService(serviceId, 5678)
        ) {

            List<VirtualService> serviceList = Arrays.asList(service1, service2, service3);

            service1.addVerifyServlet().start(Status.OUT_OF_SERVICE.toString());
            service2.addVerifyServlet().start(Status.OUT_OF_SERVICE.toString());
            service3.addVerifyServlet().start(Status.OUT_OF_SERVICE.toString());
            for (int i = 0; i < 10; i++) {

                System.out.println("Counter: " + i);
                ports.forEach(port ->{
                    given().when()
                        .put("https://localhost:10011/eureka/apps/" + serviceId + "/" + host + ":" + serviceId + ":" + port + "/status?value=OUT_OF_SERVICE")
                        .then().statusCode(SC_OK);
                });
                System.out.println("3 OUT_OF_SERVICE");
                Thread.sleep(1000);
                given().when()
                    .put("https://localhost:10011/eureka/apps/" + serviceId + "/" + host + ":" + serviceId + ":" + 5678 + "/status?value=UP")
                    .then().statusCode(SC_OK);
                System.out.println("1 UP 2 OUT_OF_SERVICE");
                Thread.sleep(1000);
                service1.getGatewayVerifyUrls().forEach(x ->
                    given()
                        .when().get(x + "/test")
                        .then().body(is("")).statusCode(is(SC_OK))
                );

//                unregister service1
                given().when().delete("https://localhost:10011/eureka/apps/" + serviceId + "/" + host + ":" + serviceId + ":" + 5678).then().statusCode(SC_OK);

                System.out.println(" 2 OUT_OF_SERVICE");
                Thread.sleep(1000);
//                set service2 UP
                given().when()
                    .put("https://localhost:10011/eureka/apps/" + serviceId + "/" + host + ":" + serviceId + ":" + 5679 + "/status?value=UP")
                    .then().statusCode(SC_OK);

//                call service2
                System.out.println("1 UP 1 OUT_OF_SERVICE");
                Thread.sleep(1000);
                service2.getGatewayVerifyUrls().forEach(x ->
                    given()
                        .when().get(x + "/test")
                        .then().body(is("")).statusCode(is(SC_OK))
                );

//                set service3 UP
                given().when()
                    .put("https://localhost:10011/eureka/apps/" + serviceId + "/" + host + ":" + serviceId + ":" + 5680 + "/status?value=UP")
                    .then().statusCode(SC_OK);

//                call service3
                System.out.println("2 UP");
                Thread.sleep(1000);
                service3.getGatewayVerifyUrls().forEach(x ->
                    given()
                        .when().get(x + "/test")
                        .then().body(is("")).statusCode(is(SC_OK))
                );

                service1.start("UP");
                System.out.println("3 UP");
                Thread.sleep(1000);
            }

        }
    }
}
