/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.cloudgateway;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.nullToEmpty;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@DiscoverableClientDependentTest
@Tag("CloudGatewayCentralRegistry")
class CentralRegistryTest implements TestWithStartedInstances {
    static final String CENTRAL_REGISTRY_PATH = "/api/v1/registry/";

    static CloudGatewayConfiguration conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();

    @BeforeAll
    @SneakyThrows
    static void setupAll() {
        //In order to avoid config customization
        ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().setInstances(2);

        SslContextConfigurer sslContextConfigurer = new SslContextConfigurer(ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStorePassword(),
            ConfigReader.environmentConfiguration().getTlsConfiguration().getClientKeystore(),
            ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStore());
        SslContext.prepareSslAuthentication(sslContextConfigurer);
    }

    @BeforeEach
    void setup() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    @SneakyThrows
    void shouldFindRegisteredGatewayInCentralApiml() {
        ValidatableResponse response = listCentralRegistry("central-apiml", "zowe.apiml.gateway", null);

        List<Map<String, Object>> services = response.extract().jsonPath().getObject("[0].services", new TypeRef<List<Map<String, Object>>>() {
        });

        assertThat(services, hasSize(1));
    }

    @Test
    void shouldFindTwoApimlInstancesInTheCentralRegistry() {
        listCentralRegistry(null, null, null)
            .body("size()", is(2));
    }

    @Test
    void shouldFindBothApimlIds() {
        List<String> apimlIds = listCentralRegistry(null, null, null)
            .extract().jsonPath().getList("apimlId");

        assertThat(apimlIds, Matchers.hasItems(Matchers.equalTo("central-apiml"), Matchers.equalTo("node-apiml")));
    }

    @Test
    @Disabled
    void shouldRejectUnauthorizedAccessToCentralRegistry() {
        //This test should be enabled after the x509 projection is implemented
        URI cloudGatewayEndpoint = buildRegistryURI(null, null, null);
        given()
            .get(cloudGatewayEndpoint)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    private ValidatableResponse listCentralRegistry(String apimlId, String apiId, String serviceId) {

        URI cloudGatewayEndpoint = buildRegistryURI(apimlId, apiId, serviceId);

        return with().given()
            .config(SslContext.clientCertApiml)
            .get(cloudGatewayEndpoint)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @SneakyThrows
    private URI buildRegistryURI(String apimlId, String apiId, String serviceId) {

        String query = String.format("%s?apiId=%s&serviceId=%s", nullToEmpty(apimlId), nullToEmpty(apiId), nullToEmpty(serviceId));

        return new URL(conf.getScheme(), conf.getHost(), conf.getPort(), CENTRAL_REGISTRY_PATH)
            .toURI().resolve(query);
    }
}
