/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.gateway;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.config.*;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@DiscoverableClientDependentTest
@Tag("GatewayCentralRegistry")
class CentralRegistryTest implements TestWithStartedInstances {
    static final String CENTRAL_REGISTRY_PATH = "/" + CoreService.GATEWAY.getServiceId() + "/api/v1/registry";

    static ServiceConfiguration conf = ConfigReader.environmentConfiguration().getCentralGatewayServiceConfiguration();
    static DiscoveryServiceConfiguration discoveryConf = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();

    @BeforeAll
    @SneakyThrows
    static void setupAll() {
        //In order to avoid config customization
        ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().setInstances(2);

        TlsConfiguration tlsCfg = ConfigReader.environmentConfiguration().getTlsConfiguration();
        SslContextConfigurer sslContextConfigurer = new SslContextConfigurer(tlsCfg.getKeyStorePassword(), tlsCfg.getClientKeystore(), tlsCfg.getKeyStore());
        SslContext.prepareSslAuthentication(sslContextConfigurer);
    }

    @BeforeEach
    void setup() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    @SneakyThrows
    void shouldFindRegisteredGatewayInCentralApiml() {
        ValidatableResponse response = listCentralRegistry("/central-apiml", "zowe.apiml.gateway", null);

        List<Map<String, Object>> services = response.extract().jsonPath().getObject("[0].services", new TypeRef<List<Map<String, Object>>>() {
        });

        assertThat(services).hasSize(1);
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

        assertThat(apimlIds).contains("central-apiml", "domain-apiml");
    }

    @Test
    void shouldFindTwoRegisteredGatewaysInTheEurekaApps() {
        TypeRef<List<ArrayList<LinkedHashMap<Object, Object>>>> typeRef = new TypeRef<>() {
        };

        ArrayList<LinkedHashMap<Object, Object>> metadata = listEurekaApps()
            .extract()
            .jsonPath()
            .getObject("applications.application.findAll { it.name == 'GATEWAY' }.instance.metadata", typeRef).get(0);

        assertThat(metadata).hasSize(4);

        assertThat(metadata)
            .extracting(map -> map.get("apiml.service.apimlId"))
            .containsExactlyInAnyOrder("central-apiml", "domain-apiml");
    }

    @Test
    void shouldRejectUnauthorizedAccessToCentralRegistry() {
        URI gatewayEndpoint = buildRegistryURI(null, null, null);
        given()
            .get(gatewayEndpoint)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldRejectUntrustedX509CertificateToAccessCentralRegistry() {
        URI gatewayEndpoint = buildRegistryURI(null, null, null);
        given()
            .config(SslContext.selfSignedUntrusted)
            .get(gatewayEndpoint)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    private ValidatableResponse listCentralRegistry(String apimlId, String apiId, String serviceId) {

        URI gatewayEndpoint = buildRegistryURI(apimlId, apiId, serviceId);

        return with().given()
            .config(SslContext.clientCertUser)
            .get(gatewayEndpoint)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @SneakyThrows
    private ValidatableResponse listEurekaApps() {

        URI eurekaApps = new URL(discoveryConf.getScheme(), discoveryConf.getHost(), discoveryConf.getPort(), "/eureka/apps")
            .toURI();

        return with().given()
            .config(SslContext.clientCertUser)
            .header(ACCEPT, APPLICATION_JSON_VALUE)
            .get(eurekaApps)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @SneakyThrows
    private URI buildRegistryURI(String apimlId, String apiId, String serviceId) {

        String query = String.format("%s?apiId=%s&serviceId=%s", nullToEmpty(apimlId), nullToEmpty(apiId), nullToEmpty(serviceId));

        return new URL(conf.getScheme(), conf.getHost(), conf.getPort(), CENTRAL_REGISTRY_PATH + query)
            .toURI();
    }

    String nullToEmpty(String s) {
        return StringUtils.defaultIfEmpty(s, "");
    }
}
