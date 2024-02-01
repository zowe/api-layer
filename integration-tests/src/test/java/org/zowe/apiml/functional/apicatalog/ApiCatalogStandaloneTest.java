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

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.config.ApiCatalogServiceConfiguration;
import org.zowe.apiml.util.config.ConfigReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("ApiCatalogStandaloneTest")
public class ApiCatalogStandaloneTest {

    private static final String GET_ALL_CONTAINERS_ENDPOINT = "/apicatalog/containers";
    private static final String GET_API_CATALOG_API_DOC_DEFAULT_ENDPOINT = "/apicatalog/apidoc/service2";
    private static final String GET_API_CATALOG_API_DOC_ENDPOINT = "/apicatalog/apidoc/service2/org.zowe v2.0.0";

    private final static String USERNAME = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-authorized").get(0).getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-authorized").get(0).getPassword();

    private String baseHost;

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
                final ValidatableResponse response = when()
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
            void whenGetApiDocDefaultEndpoint() {
                final ValidatableResponse response = when()
                                                        .get(baseHost + GET_API_CATALOG_API_DOC_DEFAULT_ENDPOINT)
                                                        .then()
                                                            .statusCode(is(SC_OK))
                                                            .contentType("application/json");
                assertEquals("Service 2 - v1 (default)", response.extract().jsonPath().get("info.title"));
            }

            @Test
            void whenGetApiDocv2Endpoint() {
                final ValidatableResponse response = when()
                                                        .get(baseHost + GET_API_CATALOG_API_DOC_ENDPOINT)
                                                        .then()
                                                            .statusCode(is(SC_OK))
                                                            .contentType("application/json");
                assertEquals("Service 2 - v2", response.extract().jsonPath().get("info.title"));
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
                        .basic(USERNAME, new String(PASSWORD))
                    .when()
                        .get(baseHost + GET_ALL_CONTAINERS_ENDPOINT)
                    .then()
                        .statusCode(is(SC_OK))
                        .contentType("application/json");
            }
        }
    }
}
