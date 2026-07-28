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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.Endpoints;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * Verifies the token-free CSRF protection driven by the {@code Sec-Fetch-Site} request header.
 * <p>
 * A state-changing (POST) request marked by the browser as {@code cross-site} is rejected with 403
 * unless its {@code Origin} is permitted by the target service's CORS configuration. The success and
 * failure cases send an <b>identical</b> cross-site request (same Origin, same {@code Sec-Fetch-Site}
 * header, same {@code /status-code} endpoint) and differ only in the target service, so the outcome is
 * attributable solely to the per-service CORS origins declared in the Eureka registration metadata
 * (see api-defs/staticclient.yml):
 * <ul>
 *     <li>{@code staticclient} declares {@code corsAllowedOrigins: https://localhost2:10010} -> request succeeds (200)</li>
 *     <li>{@code staticclient2} declares no CORS metadata -> request rejected (403)</li>
 * </ul>
 */
@DiscoverableClientDependentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsrfFetchMetadataTest {

    private static final String SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site";
    private static final String CROSS_SITE = "cross-site";
    // Origin explicitly whitelisted for the staticclient service via its Eureka metadata
    private static final String ALLOWED_ORIGIN = "https://discoverable-client:10012";
    private static final String NOT_ALLOWED_ORIGIN = "https://localhost:10012";

    private static final int OK = 200;
    private static final int FORBIDDEN = 403;

    @BeforeAll
    void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
    }

    @Test
    void givenCrossSiteRequest_whenServiceAllowsOriginViaMetadata_thenRequestSucceeds() {
        given()
            .log().all()
            .header("Origin", ALLOWED_ORIGIN)
            .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
        .when()
            .post(HttpRequestUtils.getUriFromGateway(Endpoints.STATIC_CLIENT_1_STATUS_CODE))
        .then()
            .log().all()
            .statusCode(OK);
    }

    @Test
    void givenCrossSiteRequest_whenServiceDoesNotAllowOrigin_thenRejectedAsForbidden() {
        // Identical to the succeeding request above except for the target service, which does not
        // whitelist this Origin, so the CSRF filter rejects it before it is routed downstream.
        given()
            .log().all()
            .header("Origin", NOT_ALLOWED_ORIGIN)
            .header(SEC_FETCH_SITE_HEADER, CROSS_SITE)
        .when()
            .post(HttpRequestUtils.getUriFromGateway(Endpoints.STATIC_CLIENT_1_STATUS_CODE))
        .then()
            .log().all()
            .statusCode(FORBIDDEN);
    }

    @Test
    void givenNonBrowserRequest_whenNoFetchMetadataHeader_thenNotRejected() {
        // No Sec-Fetch-Site header (a non-browser client) must never be blocked, even for a service
        // that would reject a cross-site browser request. Confirms the 403 above is driven by the header.
        given()
            .log().all()
        .when()
            .post(HttpRequestUtils.getUriFromGateway(Endpoints.STATIC_CLIENT_2_STATUS_CODE))
        .then()
            .log().all()
            .statusCode(not(equalTo(FORBIDDEN)));
    }

}
