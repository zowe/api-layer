/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.codec.WireFormat;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MetadataValidationFilterTest {

    @AcceptanceTest
    @Nested
    class GivenOnboarding extends AcceptanceTestWithBasePath {

        @Value("${server.ssl.keyStorePassword:password}")
        private char[] keystorePassword;

        @Value("${server.ssl.keyStore}")
        private String keystore;

        @BeforeEach
        void setUp() throws Exception {
            String clientKeystore = "../keystore/client/client-certs.p12";
            SslContextConfigurer configurer = new SslContextConfigurer(
                keystorePassword,
                clientKeystore,
                keystore);
            SslContext.prepareSslAuthentication(configurer);
        }

        @Test
        void whenInvalidMetadata_thenBlockWithMessage() {
            var serviceId = "MOCKSERVICE";
            Map<String, String> metadata = new HashMap<>();
            metadata.put("apiml.apiInfo.0.swaggerUrl", "https://not-allowed.example.org/api-doc");

            var mockInstance = ServiceInstance.builder()
                .instanceId("localhost:" + serviceId.toLowerCase() + ":10090")
                .appName(serviceId)
                .hostName("localhost")
                .ipAddr("127.0.0.1")
                .port(10090, true)
                .status(InstanceStatus.UP)
                .metadata(metadata)
                .build();

            var registrationRequest = new RegistryCodec().encode(mockInstance, WireFormat.JSON_FULL);

            var responseBody = given()
                .config(SslContext.clientCertApiml)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(registrationRequest)
            .when()
                .post("https://localhost:10011/eureka/apps/" + serviceId)
            .then()
                .log().all()
                .statusCode(SC_BAD_REQUEST).and().extract().body().asString();

            assertEquals("""
                {"messages":[{"messageType":"WARNING","messageNumber":"ZWEAM604W","messageContent":"Invalid metadata found in registration","messageAction":"Validate URL / IP Address against the allowed domains configuration.","messageReason":"The URL / IP Address is not allowed","messageKey":"org.zowe.apiml.common.metadataNotAllowedInRegistration"}]}
                """.trim(), responseBody);

            given()
                .config(SslContext.clientCertApiml)
                .accept(ContentType.JSON)
            .when()
                .get("https://localhost:10011/eureka/apps/" + serviceId)
            .then()
                .statusCode(404);
        }

    }

}
