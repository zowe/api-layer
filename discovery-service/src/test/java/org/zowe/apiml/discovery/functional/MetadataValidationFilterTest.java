/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.functional;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.discovery.converters.EurekaJacksonCodec;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles({"test", "https"})
class MetadataValidationFilterTest extends DiscoveryFunctionalTest {

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

    @Override
    protected String getProtocol() {
        return "https";
    }

    @Nested
    class GivenOnboarding {

        @Test
        void whenInvalidMetadata_thenBlockWithMessage() {
            var serviceId = "MOCKSERVICE";
            Map<String, String> metadata = new HashMap<>();
            metadata.put("apiml.apiInfo.0.swaggerUrl", "https://not-allowed.example.org/api-doc");

            var mockInstance = InstanceInfo.Builder.newBuilder()
                .setInstanceId("localhost:" + serviceId.toLowerCase() + ":10090")
                .setAppName(serviceId)
                .setHostName("localhost")
                .setIPAddr("127.0.0.1")
                .setPort(10090)
                .setStatus(InstanceInfo.InstanceStatus.UP)
                .setDataCenterInfo(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn))
                .setMetadata(metadata)
                .build();

            var registrationRequest = EurekaJacksonCodec.getInstance().writeToString(mockInstance);

            var responseBody = given()
                .config(SslContext.clientCertApiml)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(registrationRequest)
            .when()
                .post(getDiscoveryUriWithPath("/eureka/apps/" + serviceId))
            .then()
                .log().all()
                .statusCode(SC_INTERNAL_SERVER_ERROR).and().extract().body().asString();

            assertEquals("""
                {"messages":[{"messageType":"WARNING","messageNumber":"ZWEAM604W","messageContent":"Invalid metadata found in registration","messageAction":"Validate URL / IP Address against the allowed domains configuration.","messageReason":"The URL / IP Address is not allowed","messageKey":"org.zowe.apiml.common.metadataNotAllowedInRegistration"}]}
                """.trim(), responseBody);

            given()
                .config(SslContext.clientCertApiml)
                .accept(ContentType.JSON)
            .when()
                .get(getDiscoveryUriWithPath("/eureka/apps/" + serviceId))
            .then()
                .statusCode(404);
        }

    }

}
