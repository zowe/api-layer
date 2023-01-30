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

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zowe.apiml.util.http.HttpRequestUtils.getUriFromGateway;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.zowe.apiml.util.config.ApiCatalogServiceConfiguration;
import org.zowe.apiml.util.config.ConfigReader;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.response.Validatable;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

public class ApiCatalogStandaloneTest {

    private static final String GET_ALL_CONTAINERS_ENDPOINT = "/apicatalog/containers";
    private static final String GET_API_CATALOG_API_DOC_DEFAULT_ENDPOINT = "/apicatalog/apidoc/apicatalog";
    private static final String GET_API_CATALOG_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/apicatalog/zowe.apiml.apicatalog v1.0.0";
    private static final String CATALOG_SERVICE_ID = "apicatalog";
    private static final String CATALOG_SERVICE_ID_PATH = "/" + CATALOG_SERVICE_ID;
    private static final String CATALOG_PREFIX = "/api/v1";
    private static final String CATALOG_STATIC_REFRESH_ENDPOINT = "/static-api/refresh";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v1.0.0";

    private final static String USERNAME = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-authorized").get(0).getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-authorized").get(0).getPassword();

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
        String scheme = configuration.getScheme();
        int port = configuration.getPort();
        baseHost = scheme + "://" + host + ":" + port;
        RestAssured.config = RestAssured.config().sslConfig(SSLConfig.sslConfig());
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class Containers {

        @Nested
        class HasRegisteredServices {

            @Test
            void whenGetContainers() throws IOException {
                final ValidatableResponse response = given()
                                                        .when()
                                                            .get(baseHost + GET_ALL_CONTAINERS_ENDPOINT)
                                                        .then()
                                                            .statusCode(is(SC_OK))
                                                            .contentType("application/json");

                List<Map<String, Object>> list = response.extract().jsonPath().getList("$.");
                assertEquals(2, list.size());
                assertEquals("Mocked Services from file", list.get(0).get("title"));
                assertEquals("Another mocked Services from file", list.get(1).get("title"));
            }
        }

        @Nested
        class ApiDocIsAvailable {

            @Test
            void test() {
                // verify accessing the /containers endpoint to validate that the api doc link is there, and it is accessible (I can fetch the contents of the swagger/open api)
            }

        }
    }

    @Nested
    class Access {

        @Nested
        class AuthenticationIsNotRequired {

            @Test
            void givenBasicAuthenticationIsProvided() {
                given()
                    .auth()
                        .basic(USERNAME, PASSWORD)
                    .when()
                        .get(baseHost + GET_ALL_CONTAINERS_ENDPOINT)
                    .then()
                        .statusCode(is(SC_OK))
                        .contentType("application/json");
            }
        }
    }
}
