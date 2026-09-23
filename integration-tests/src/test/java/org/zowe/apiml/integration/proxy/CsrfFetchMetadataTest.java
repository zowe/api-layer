/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.proxy;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.Endpoints;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Verifies the token-free CSRF protection driven by the {@code Sec-Fetch-*} request headers.
 * <p>
 * A request whose {@code Sec-Fetch-Site} is neither absent nor {@code same-origin}/{@code none} is
 * allowed only when one of two things holds, and is rejected with 403 otherwise:
 * <ul>
 *     <li>the decision can be deferred to the CORS layer - the Gateway has CORS enabled, the request
 *     carries an {@code Origin}, and a CORS configuration is registered for the requested path, or</li>
 *     <li>it is a safe top-level navigation - the {@code Sec-Fetch-Mode} is one of the configured safe
 *     navigation modes ({@code navigate}, {@code same-origin} by default) <b>and</b> the HTTP method
 *     cannot change state on its own ({@code GET}/{@code HEAD}).</li>
 * </ul>
 * The nesting below mirrors that decision tree.
 * <p>
 * The CORS-deferral cases send an <b>identical</b> cross-site request (same {@code Sec-Fetch-Site}
 * header, same {@code /status-code} endpoint) and differ only in the target service or in the presence
 * of the {@code Origin}, so the outcome is attributable solely to what the CORS layer can judge. The
 * per-service origins come from the Eureka registration metadata (see api-defs/staticclient.yml):
 * <ul>
 *     <li>{@code staticclient} declares {@code corsAllowedOrigins: https://discoverable-client:10012} -> request succeeds (200)</li>
 *     <li>{@code staticclient2} declares no allowed origin -> request rejected (403)</li>
 * </ul>
 * The navigation cases deliberately send <b>no</b> {@code Origin}, which takes the CORS layer out of
 * the picture entirely (a request without an {@code Origin} is not a CORS request), so their outcome is
 * attributable solely to the Fetch Metadata policy.
 */
@DiscoverableClientDependentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsrfFetchMetadataTest {

    private static final String SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site";
    private static final String SEC_FETCH_MODE_HEADER = "Sec-Fetch-Mode";
    private static final String SEC_FETCH_DEST_HEADER = "Sec-Fetch-Dest";
    private static final String ORIGIN_HEADER = "Origin";

    private static final String CROSS_SITE = "cross-site";
    private static final String NAVIGATE = "navigate";

    private static final String ALLOWED_ORIGIN = "https://discoverable-client:10012";
    private static final String NOT_ALLOWED_ORIGIN = "https://localhost:10012";

    private static final String UNREGISTERED_PATH = "/nosuchservice/api/v1/status-code";

    private static final String REJECTION_BODY = "Access denied.";

    private static final int OK = 200;
    private static final int FORBIDDEN = 403;

    @BeforeAll
    void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
    }

    private RequestSpecification request() {
        return given().log().ifValidationFails();
    }

    private RequestSpecification crossSiteNavigation() {
        return request()
            .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
            .header(SEC_FETCH_MODE_HEADER, NAVIGATE)
            .header(SEC_FETCH_DEST_HEADER, "document");
    }

    private URI statusCodeOfStaticClient() {
        return HttpRequestUtils.getUriFromGateway(Endpoints.STATIC_CLIENT_1_STATUS_CODE);
    }

    private URI statusCodeOfStaticClient2() {
        return HttpRequestUtils.getUriFromGateway(Endpoints.STATIC_CLIENT_2_STATUS_CODE);
    }

    @Nested
    class GivenSafeSecFetchSite {

        @Test
        void whenNoFetchMetadataHeaderAtAll_thenNotRejected() {
            request()
                .when()
                .post(statusCodeOfStaticClient2())
                .then()
                .log().ifValidationFails()
                .statusCode(not(equalTo(FORBIDDEN)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"same-origin", "none", "Same-Origin", "NONE"})
        void whenStateChangingRequest_thenNotRejected(String site) {
            request()
                .header(SEC_FETCH_SITE_HEADER, site)
                .when()
                .post(statusCodeOfStaticClient2())
                .then()
                .log().ifValidationFails()
                .statusCode(OK);
        }
    }

    @Nested
    class GivenSameSiteRequest {

        @Test
        void whenStateChangingRequest_thenRejectedAsForbidden() {
            request()
                .header(SEC_FETCH_SITE_HEADER, "same-site")
                .when()
                .post(statusCodeOfStaticClient())
                .then()
                .log().ifValidationFails()
                .statusCode(FORBIDDEN);
        }
    }

    @Nested
    class GivenCrossSiteRequest {

        @Nested
        class WhenDeferredToCors {

            @Test
            void givenServiceAllowsOriginViaMetadata_thenRequestSucceeds() {
                request()
                    .header(ORIGIN_HEADER, ALLOWED_ORIGIN)
                    .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
                    .when()
                    .post(statusCodeOfStaticClient())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(OK);
            }

            @Test
            void givenServiceDoesNotAllowOrigin_thenRejectedAsForbidden() {
                request()
                    .header(ORIGIN_HEADER, NOT_ALLOWED_ORIGIN)
                    .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
                    .when()
                    .post(statusCodeOfStaticClient2())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(FORBIDDEN);
            }

            @Test
            void givenNoOriginToDeferOn_thenRejectedAsForbidden() {
                request()
                    .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
                    .when()
                    .post(statusCodeOfStaticClient())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(FORBIDDEN);
            }

            @Test
            void givenNoCorsConfigurationForPath_thenRejectedAsForbidden() {
                request()
                    .header(ORIGIN_HEADER, ALLOWED_ORIGIN)
                    .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
                    .when()
                    .post(HttpRequestUtils.getUriFromGateway(UNREGISTERED_PATH))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(FORBIDDEN);
            }
        }

        @Nested
        class WhenTopLevelNavigation {

            @ParameterizedTest
            @ValueSource(strings = {"GET", "HEAD"})
            void givenSafeMethod_thenAllowed(String method) {
                crossSiteNavigation()
                    .when()
                    .request(Method.valueOf(method), statusCodeOfStaticClient())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(OK);
            }

            @ParameterizedTest
            @ValueSource(strings = {"POST", "PUT", "DELETE", "PATCH"})
            void givenStateChangingMethod_thenRejectedAsForbidden(String method) {
                crossSiteNavigation()
                    .when()
                    .request(Method.valueOf(method), statusCodeOfStaticClient())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(FORBIDDEN);
            }

            @ParameterizedTest
            @ValueSource(strings = {"document", "iframe", "empty", "IFRAME"})
            void givenAnyDestination_thenAllowedByDefault(String destination) {
                request()
                    .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
                    .header(SEC_FETCH_MODE_HEADER, NAVIGATE)
                    .header(SEC_FETCH_DEST_HEADER, destination)
                    .when()
                    .get(statusCodeOfStaticClient())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(OK);
            }
        }

        @Nested
        class WhenNotATopLevelNavigation {

            @ParameterizedTest
            @ValueSource(strings = {"navigate", "same-origin", "NAVIGATE"})
            void givenSafeNavigationMode_thenAllowed(String mode) {
                request()
                    .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
                    .header(SEC_FETCH_MODE_HEADER, mode)
                    .when()
                    .get(statusCodeOfStaticClient())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(OK);
            }

            @ParameterizedTest
            @ValueSource(strings = {"cors", "no-cors", "websocket"})
            void givenModeIsNotANavigation_thenRejectedAsForbidden(String mode) {
                request()
                    .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
                    .header(SEC_FETCH_MODE_HEADER, mode)
                    .when()
                    .get(statusCodeOfStaticClient())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(FORBIDDEN);
            }

            @Test
            void givenNoModeHeader_thenRejectedAsForbidden() {
                request()
                    .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
                    .when()
                    .get(statusCodeOfStaticClient())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(FORBIDDEN);
            }
        }

        @Nested
        class WhenRejected {

            @Test
            void thenRespondsWithPlainTextAccessDenied() {
                request()
                    .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
                    .when()
                    .post(statusCodeOfStaticClient())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(FORBIDDEN)
                    .contentType(startsWith("text/plain"))
                    .body(equalTo(REJECTION_BODY));
            }
        }
    }

}
