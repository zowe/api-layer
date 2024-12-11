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

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import lombok.SneakyThrows;
import net.minidev.json.JSONArray;
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.zowe.apiml.util.SecurityUtils.GATEWAY_TOKEN_COOKIE_NAME;
import static org.zowe.apiml.util.SecurityUtils.gatewayToken;

@DiscoverableClientDependentTest
@Tag("GatewayCentralRegistry")
class CentralRegistryTest implements TestWithStartedInstances {
    static final String CENTRAL_REGISTRY_PATH = "/" + CoreService.GATEWAY.getServiceId() + "/api/v1/registry";
    public static final String APIML_CONTAINER_PATH = "/" + CoreService.API_CATALOG.getServiceId() + "/api/v1/containers/apimediationlayer";
    public static final String DOMAIN_APIML = "domain-apiml";
    public static final String CENTRAL_APIML = "central-apiml";

    static ServiceConfiguration conf = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
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

        List<Map<String, Object>> services = response.extract().jsonPath().getObject("[0].services", new TypeRef<>() {
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

        assertThat(apimlIds).contains(CENTRAL_APIML, DOMAIN_APIML);
    }

    @Test
    void shouldFindTwoRegisteredGatewaysInTheEurekaApps() {
        TypeRef<List<ArrayList<LinkedHashMap<Object, Object>>>> typeRef = new TypeRef<>() {
        };

        ArrayList<LinkedHashMap<Object, Object>> metadata = listEurekaApps()
            .extract()
            .jsonPath()
            .getObject("applications.application.findAll { it.name == 'GATEWAY' }.instance.metadata", typeRef).get(0);

        assertThat(metadata).hasSize(2);

        assertThat(metadata)
            .extracting(map -> map.get("apiml.service.apimlId"))
            .containsOnly(CENTRAL_APIML, DOMAIN_APIML);
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

    @Test
    void shouldContainCorrectBasePaths() throws MalformedURLException, URISyntaxException {
        URI containers = new URL(conf.getScheme(), conf.getHost(), conf.getPort(), APIML_CONTAINER_PATH)
            .toURI();

        final String jwt = gatewayToken();
        String responseBody = with().given()
            .header(ACCEPT, APPLICATION_JSON_VALUE)
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
            .get(containers)
            .then()
            .statusCode(200)
            .contentType("application/json")
            .extract()
            .body()
            .asString();

        DocumentContext jsonContext = JsonPath.parse(responseBody);

        JSONArray gatewayBasePath = jsonContext.read("$[0].services[?(@.serviceId == 'gateway')].basePath");
        assertNotNull(gatewayBasePath, String.format("BasePath for central gw should not be null but it was '%s'", gatewayBasePath));
        assertFalse(gatewayBasePath.isEmpty(), String.format("BasePath for central gw should not be empty but it was '%s'", gatewayBasePath));
        assertEquals("/", gatewayBasePath.get(0));
        JSONArray domainGatewayBasePath = jsonContext.read("$[0].services[?(@.serviceId == 'domain-apiml')].basePath");
        assertNotNull(domainGatewayBasePath, String.format("BasePath for domain gw should not be null but it was '%s'", domainGatewayBasePath));
        assertFalse(domainGatewayBasePath.isEmpty(), String.format("BasePath for domain gw should not be empty but it was '%s'", domainGatewayBasePath));
        assertEquals("/" + DOMAIN_APIML, domainGatewayBasePath.get(0));
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
