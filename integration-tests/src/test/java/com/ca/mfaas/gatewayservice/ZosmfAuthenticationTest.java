/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gatewayservice;

import com.ca.mfaas.util.categories.AdditionalLocalTest;
import com.ca.mfaas.util.service.DiscoveryUtils;
import com.ca.mfaas.util.service.VirtualService;
import io.restassured.RestAssured;
import lombok.AllArgsConstructor;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

import static com.ca.mfaas.gatewayservice.SecurityUtils.getConfiguredSslConfig;
import static io.restassured.RestAssured.given;

/**
 * Those tests simulate different version of z/OSMF.
 *
 * For right execution is required:
 *  - set provider in gateway to zosmf with serviceId zosmfca32
 *  - start discovery service and gateway locally
 */
@RunWith(JUnit4.class)
@Category(AdditionalLocalTest.class)
public class ZosmfAuthenticationTest {

    private static final String ZOSMF_ID = "zosmfca32";
    private static final String LOGIN_ENDPOINT = "/api/v1/gateway/auth/login";

    private static final String USER_ID = "user";
    private static final String PASSWORD = "secret";
    private static final String WRONG_PASSWORD = "public";

    private static final int TIMEOUT_REGISTRATION = 10;

    @Before
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Test
    public void testNoResponse() throws Exception {
        try (VirtualService zosmf = new VirtualService(ZOSMF_ID)) {
            zosmf
                .addServlet("info", "/zosmf/info", new AuthServletGet(
                    null, null, "LtpaToken2=ltpaToken", HttpStatus.OK
                ))
                .addRoute("/api", "/")
                .start()
                .waitForGatewayRegistration(1, TIMEOUT_REGISTRATION);

            given()
                .auth().preemptive().basic(USER_ID, PASSWORD)
                .post(DiscoveryUtils.getGatewayUrls().get(0) + LOGIN_ENDPOINT)
            .then().statusCode(HttpStatus.NO_CONTENT.value());
        }
    }

    @Test
    public void testOldAuthenticationEndpoint() throws Exception {
        try (VirtualService zosmf = new VirtualService(ZOSMF_ID)) {
            zosmf
                .addServlet("info", "/zosmf/info", new AuthServletGet(
                    "{\"zosmf_version\":\"25\",\"zosmf_full_version\": \"25.2\",\"zosmf_saf_realm\": \"SAFRealm\",\"otherAttribute\":\"someValue\"}",
                    null, "LtpaToken2=ltpaToken", HttpStatus.OK
                ))
                .addRoute("/api", "/")
                .start()
                .waitForGatewayRegistration(1, TIMEOUT_REGISTRATION);

            given()
                .auth().preemptive().basic(USER_ID, PASSWORD)
                .post(DiscoveryUtils.getGatewayUrls().get(0) + LOGIN_ENDPOINT)
                .then().statusCode(HttpStatus.NO_CONTENT.value()).cookie("apimlAuthenticationToken");
        }
    }

    @Test
    public void testOldAuthenticationEndpointInvalid() throws Exception {
        try (VirtualService zosmf = new VirtualService(ZOSMF_ID)) {
            zosmf
                .addServlet("info", "/zosmf/info", new AuthServletGet(
                    "{\"zosmf_version\":\"25\",\"zosmf_full_version\": \"25.2\",\"zosmf_saf_realm\": \"SAFRealm\",\"otherAttribute\":\"someValue\"}",
                    null, "LtpaToken2=ltpaToken", HttpStatus.OK
                ))
                .addRoute("/api", "/")
                .start()
                .waitForGatewayRegistration(1, TIMEOUT_REGISTRATION);

            given()
                .auth().preemptive().basic(USER_ID, WRONG_PASSWORD)
                .post(DiscoveryUtils.getGatewayUrls().get(0) + LOGIN_ENDPOINT)
                .then().statusCode(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Test
    public void testNewAuthenticationEndpointLtpa() throws Exception {
        try (VirtualService zosmf = new VirtualService(ZOSMF_ID)) {
            zosmf
                .addServlet("info", "/zosmf/info", new AuthServletGet(
                    "{\"zosmf_version\":\"27\",\"zosmf_full_version\": \"27.0\",\"zosmf_saf_realm\": \"SAFRealm\",\"otherAttribute\":\"someValue\"}",
                    null, null, HttpStatus.OK
                ))
                .addServlet("auth", "/zosmf/services/authenticate", new AuthServletPost(
                    null,
                    null, "LtpaToken2=ltpaToken", HttpStatus.UNAUTHORIZED
                ))
                .addRoute("/api", "/")
                .start()
                .waitForGatewayRegistration(1, TIMEOUT_REGISTRATION);

            given()
                .auth().preemptive().basic(USER_ID, PASSWORD)
                .post(DiscoveryUtils.getGatewayUrls().get(0) + LOGIN_ENDPOINT)
                .then().statusCode(HttpStatus.NO_CONTENT.value()).cookie("apimlAuthenticationToken");
        }
    }

    @Test
    public void testNewAuthenticationEndpointJwt() throws Exception {
        try (VirtualService zosmf = new VirtualService(ZOSMF_ID)) {
            zosmf
                .addServlet("info", "/zosmf/info", new AuthServletGet(
                    "{\"zosmf_version\":\"27\",\"zosmf_full_version\": \"27.0\",\"zosmf_saf_realm\": \"SAFRealm\",\"otherAttribute\":\"someValue\"}",
                    null, null, HttpStatus.OK
                ))
                .addServlet("auth", "/zosmf/services/authenticate", new AuthServletPost(
                    null,
                    null, "jwtToken=jwtToken", HttpStatus.UNAUTHORIZED
                ))
                .addRoute("/api", "/")
                .start()
                .waitForGatewayRegistration(1, TIMEOUT_REGISTRATION);

            given()
                .auth().preemptive().basic(USER_ID, PASSWORD)
                .post(DiscoveryUtils.getGatewayUrls().get(0) + LOGIN_ENDPOINT)
                .then().statusCode(HttpStatus.NO_CONTENT.value()).cookie("apimlAuthenticationToken", "jwtToken");
        }
    }

    @Test
    public void testNewAuthenticationEndpointInvalid() throws Exception {
        try (VirtualService zosmf = new VirtualService(ZOSMF_ID)) {
            zosmf
                .addServlet("info", "/zosmf/info", new AuthServletGet(
                    "{\"zosmf_version\":\"27\",\"zosmf_full_version\": \"27.0\",\"zosmf_saf_realm\": \"SAFRealm\",\"otherAttribute\":\"someValue\"}",
                    null, null, HttpStatus.OK
                ))
                .addServlet("auth", "/zosmf/services/authenticate", new AuthServletPost(
                    null,
                    null, "jwtToken=jwtToken", HttpStatus.UNAUTHORIZED
                ))
                .addRoute("/api", "/")
                .start()
                .waitForGatewayRegistration(1, TIMEOUT_REGISTRATION);

            given()
                .auth().preemptive().basic(USER_ID, WRONG_PASSWORD)
                .post(DiscoveryUtils.getGatewayUrls().get(0) + LOGIN_ENDPOINT)
                .then().statusCode(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @AllArgsConstructor
    private static class AuthServlet extends HttpServlet {

        protected final String bodyOnSuccess;
        protected final String bodyOnError;
        protected final String cookiesOnSuccess;
        protected final HttpStatus unauthorizedStatus;

        protected boolean isAuthorized(HttpServletRequest req) {
            final String authorization = req.getHeader(HttpHeaders.AUTHORIZATION).split(" ", 2)[1];
            final String basic = new String(Base64.getDecoder().decode(authorization));

            return
                USER_ID.equalsIgnoreCase(basic.split(":", 2)[0]) &&
                    PASSWORD.equalsIgnoreCase(basic.split(":", 2)[1]);
        }

        protected void success(HttpServletResponse resp, boolean authorized) throws IOException {
            if (authorized && (cookiesOnSuccess != null)) {
                resp.setHeader(HttpHeaders.SET_COOKIE, cookiesOnSuccess);
            }
            if (bodyOnSuccess != null) {
                resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
                resp.getOutputStream().write(bodyOnSuccess.getBytes());
                resp.setStatus(HttpStatus.OK.value());
            } else {
                resp.setStatus(HttpStatus.NO_CONTENT.value());
            }
        }

        protected void fail(HttpServletResponse resp) throws IOException {
            if (bodyOnError != null) {
                resp.getOutputStream().write(bodyOnSuccess.getBytes());
            }
            resp.setStatus(unauthorizedStatus.value());
        }
    }

    private static class AuthServletGet extends AuthServlet {

        public AuthServletGet(String bodyOnSuccess, String bodyOnError, String cookiesOnSuccess, HttpStatus unauthorizedStatus) {
            super(bodyOnSuccess, bodyOnError, cookiesOnSuccess, unauthorizedStatus);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            if (req.getHeader("X-CSRF-ZOSMF-HEADER") == null) {
                resp.setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }

            if (req.getHeader(HttpHeaders.AUTHORIZATION) != null) {
                if (isAuthorized(req)) {
                    success(resp, true);
                } else {
                    fail(resp);
                }
            } else {
                success(resp, false);
            }
        }

    }

    private static class AuthServletPost extends AuthServlet {

        public AuthServletPost(String bodyOnSuccess, String bodyOnError, String cookiesOnSuccess, HttpStatus unauthorizedStatus) {
            super(bodyOnSuccess, bodyOnError, cookiesOnSuccess, unauthorizedStatus);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            if (req.getHeader("X-CSRF-ZOSMF-HEADER") == null) {
                resp.setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }

            if (req.getHeader(HttpHeaders.AUTHORIZATION) == null) {
                fail(resp);
            } else if (isAuthorized(req)) {
                success(resp, true);
            } else {
                fail(resp);
            }
        }

    }

}
