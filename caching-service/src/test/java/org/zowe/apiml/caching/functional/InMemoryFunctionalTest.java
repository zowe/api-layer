/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class InMemoryFunctionalTest {

    @Value("${server.ssl.keyPassword}")
    char[] password;
    @Value("${server.ssl.keyStore}")
    String client_cert_keystore;
    @Value("${server.ssl.keyStore}")
    String keystore;

    @Value("${apiml.service.hostname:localhost}")
    String hostname;

    @LocalServerPort
    int port;

    public static final String SERVICE_ID_HEADER = "X-Certificate-DistinguishedName";

    String contextPath = "/api/v1";

    String getUri(String endpoint) {
        return String.format("https://%s:%s%s%s", hostname, port, contextPath, endpoint);
    }

    @BeforeEach
    void setup() throws Exception {
        SslContextConfigurer configurer = new SslContextConfigurer(password, client_cert_keystore, keystore);
        SslContext.prepareSslAuthentication(configurer);
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WhenCallingByTrustedClient {

        @Test
        @Order(1)
        void createEntry() throws Exception {
            KeyValue keyValue = new KeyValue("first-key", "anyValue");
            ObjectMapper mapper = new ObjectMapper();
            System.out.println(1);
            given().config(SslContext.clientCertApiml)
                .body(mapper.writeValueAsString(keyValue))
                .header("Content-type", "application/json")
                .header(SERVICE_ID_HEADER, "service1")
                .post(getUri("/cache"))
                .then()
                .statusCode(HttpStatus.CREATED.value());
        }

        @Test
        @Order(2)
        void readAllEntries() {
            System.out.println(2);
            given().config(SslContext.clientCertApiml)
                .header("Content-type", "application/json")
                .header(SERVICE_ID_HEADER, "service1")
                .get(getUri("/cache"))
                .then()
                .body("first-key.key", is("first-key"))
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        @Order(3)
        void readEntry() {
            given().config(SslContext.clientCertApiml)
                .header("Content-type", "application/json")
                .header(SERVICE_ID_HEADER, "service1")
                .get(getUri("/cache/first-key"))
                .then()
                .body("value", is("anyValue"))
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        @Order(4)
        void updateEntry() throws Exception {
            KeyValue keyValue = new KeyValue("first-key", "newValue");
            ObjectMapper mapper = new ObjectMapper();

            given().config(SslContext.clientCertApiml)
                .body(mapper.writeValueAsString(keyValue))
                .header("Content-type", "application/json")
                .header(SERVICE_ID_HEADER, "service1")
                .put(getUri("/cache"))
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            given().config(SslContext.clientCertApiml)
                .header("Content-type", "application/json")
                .header(SERVICE_ID_HEADER, "service1")
                .get(getUri("/cache/first-key"))
                .then()
                .body("value", is("newValue"))
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        @Order(5)
        void deleteEntry() {
            given().config(SslContext.clientCertApiml)
                .header("Content-type", "application/json")
                .header(SERVICE_ID_HEADER, "service1")
                .delete(getUri("/cache/first-key"))
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
        }
    }

    @Nested
    class WhenClientIsNotTrusted {
        @Test
        @Order(6)
        void responseIsForbidden() {
            given().config(SslContext.clientCertUnknownUser)
                .header("Content-type", "application/json")
                .header(SERVICE_ID_HEADER, "service1")
                .get(getUri("/cache"))
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }
    }
}
