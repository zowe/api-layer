/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.apicatalog;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.categories.CatalogTest;
import org.zowe.apiml.util.config.ApiCatalogServiceConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.response.Validatable;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.http.HttpRequestUtils.getUriFromGateway;
import static org.hamcrest.MatcherAssert.assertThat;

@CatalogTest
public class ApiCatalogStandaloneTest {

    private static final String CATALOG_SERVICE_ID = "apicatalog";
    private static final String CATALOG_SERVICE_ID_PATH = "/" + CATALOG_SERVICE_ID;
    private static final String CATALOG_PREFIX = "/api/v1";

    private static final String CATALOG_STATIC_REFRESH_ENDPOINT = "/static-api/refresh";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v1.0.0";

    private String baseHost;

    @FunctionalInterface
    private interface Request {
        Validatable<?, ?> execute(RequestSpecification when, String endpoint);
    }

    static Stream<Arguments> requestsToTest() {
        return Stream.of(
            Arguments.of(CATALOG_APIDOC_ENDPOINT, (Request) (when, endpoint) ->
                when.urlEncodingEnabled(false) // space in URL gets encoded by getUriFromGateway
                    .get(getUriFromGateway(CATALOG_SERVICE_ID_PATH + CATALOG_PREFIX + endpoint))
            ),
            Arguments.of(CATALOG_STATIC_REFRESH_ENDPOINT, (Request) (when, endpoint) -> when.post(getUriFromGateway(CATALOG_SERVICE_ID_PATH + CATALOG_PREFIX + endpoint)))
        );
    }

    @BeforeAll
    static void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @BeforeEach
    void setUp() {
        ApiCatalogServiceConfiguration configuration = ConfigReader.environmentConfiguration().getApiCatalogStandaloneConfiguration();
        String host = configuration.getHost();
        int port = configuration.getPort();
        baseHost = host + ":" + port;
        RestAssured.config = RestAssured.config().sslConfig(SSLConfig.sslConfig());
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class CatalogContent {

        @Nested
        class HasRegisteredServices {

            @Test
            void givenItsStandalone() {

            }
        }
    }

    @Nested
    class Access {

        @Nested
        class AuthenticationIsNotRequired {

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogStandaloneTest#requestsToTest")
            void givenNoAuthenticationIsProvided(String endpoint, Request request) {
                request.execute(
                        given()
                            .when(),
                        endpoint
                    )
                    .then()
                    .statusCode(is(SC_OK));
            }

            @ParameterizedTest
            void givenBasicAuthenticationIsProvided() {

            }
        }
    }

    private HttpResponse getResponse(String endpoint, int returnCode) throws IOException {
        HttpGet request = HttpRequestUtils.getRequest(endpoint);
        HttpResponse response = HttpClientUtils.client().execute(request);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(returnCode));

        return response;
    }
}
