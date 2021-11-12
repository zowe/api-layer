/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.authentication.providers;

import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.zOSMFAuthTest;
import org.zowe.apiml.util.config.*;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.util.SecurityUtils.COOKIE_NAME;
import static org.zowe.apiml.util.SecurityUtils.assertValidAuthToken;
import static org.zowe.apiml.util.requests.Endpoints.*;

@zOSMFAuthTest
class ZosmfLoginTest implements TestWithStartedInstances {
    private final static String ZOSMF_SERVICE_ID = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getServiceId();
    private final static String ZOSMF_ENDPOINT = "/" + ZOSMF_SERVICE_ID + "/api/zosmf/restfiles/ds";
    private URI LOGIN_ENDPOINT_URL = HttpRequestUtils.getUriFromGateway(ROUTED_LOGIN);

    @BeforeAll
    static void setupClients() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();

        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
    }

    @Nested
    class WhenCallingZosmfAfterAuthentication {
        @Nested
        class ReturnExistingDatasets {
            @Test
            void givenValidCertificate() {
                Cookie cookie =
                    given()
                        .config(SslContext.clientCertValid)
                    .when()
                        .post(LOGIN_ENDPOINT_URL)
                    .then()
                        .statusCode(is(SC_NO_CONTENT))
                        .cookie(COOKIE_NAME, not(isEmptyString()))
                        .extract().detailedCookie(COOKIE_NAME);

                assertValidAuthToken(cookie, Optional.of("APIMTST"));

                String dsname1 = "SYS1.PARMLIB";
                String dsname2 = "SYS1.PROCLIB";

                List<NameValuePair> arguments = new ArrayList<>();
                arguments.add(new BasicNameValuePair("dslevel", "sys1.p*"));

                given()
                    .config(SslContext.tlsWithoutCert)
                    .cookie(cookie)
                    .header("X-CSRF-ZOSMF-HEADER", "")
                .when()
                    .get(HttpRequestUtils.getUriFromGateway(ZOSMF_ENDPOINT, arguments))
                .then()
                    .statusCode(is(SC_OK))
                    .body(
                        "items.dsname", hasItems(dsname1, dsname2)
                    );
            }
        }
    }

    @Nested
    class WhenUserAuthenticates {
        @Nested
        class ReturnValidToken {
            @Test
            void givenClientX509Cert(URI loginUrl) {
                Cookie cookie =
                    given()
                        .config(SslContext.clientCertValid)
                    .when()
                        .post(LOGIN_ENDPOINT_URL)
                    .then()
                        .statusCode(is(SC_NO_CONTENT))
                        .cookie(COOKIE_NAME, not(isEmptyString()))
                        .extract()
                        .detailedCookie(COOKIE_NAME);

                assertValidAuthToken(cookie, Optional.of("APIMTST"));
            }

            @Test
            void givenValidClientCertAndInvalidBasic(URI loginUrl) {
                Cookie cookie =
                    given()
                        .config(SslContext.clientCertValid)
                        .auth().basic("Bob", "The Builder")
                    .when()
                        .post(LOGIN_ENDPOINT_URL)
                    .then()
                        .statusCode(is(SC_NO_CONTENT))
                        .cookie(COOKIE_NAME, not(isEmptyString()))
                        .extract().detailedCookie(COOKIE_NAME);

                assertValidAuthToken(cookie, Optional.of("APIMTST"));
            }
        }
    }
}
