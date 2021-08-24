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
import org.junit.jupiter.api.*;
import org.springframework.http.HttpMethod;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.*;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.GatewayRequests;
import org.zowe.apiml.util.requests.RequestParams;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.util.SecurityUtils.*;

@GeneralAuthenticationTest
@SAFAuthTest
@zOSMFAuthTest
public class RefreshTest implements TestWithStartedInstances {

    public static final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();

    public static final URI REFRESH_URL = HttpRequestUtils.getUriFromGateway(authConfigurationProperties.getGatewayRefreshEndpointNewFormat());
    private final GatewayRequests requests = new GatewayRequests(
        ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost(),
        String.valueOf(ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort()));

    @BeforeAll
    public static void init() throws Exception {
        SslContext.prepareSslAuthentication();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class GivenLegalAccessModes {

        @Test
        void whenJwtTokenPostedCanBeRefreshedAndOldCookieInvalidated() throws URISyntaxException {

            String gatewayToken = gatewayToken();
            RequestParams params = RequestParams.builder()
                .method(HttpMethod.POST)
                .uri(requests.getGatewayUriWithPath(authConfigurationProperties.getGatewayRefreshEndpointNewFormat()))
                .authentication(gatewayToken).build();


            requests.route(params).then().statusCode(204).cookie(COOKIE_NAME, allOf(
                not(gatewayToken), not(isEmptyOrNullString())
            ));

            assertIfLogged(gatewayToken, false);
        }

    }


}
