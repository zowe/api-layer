/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discoveryservice;

import com.ca.mfaas.gatewayservice.SecurityUtils;
import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.DiscoveryServiceConfiguration;
import com.ca.mfaas.utils.config.TlsConfiguration;
import com.netflix.discovery.shared.transport.jersey.SSLSocketFactoryAdapter;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * This test suite must be run with HTTPS on and Certificate validation ON for Discovery service
 */
public class EurekaInstancesIntegrationTest {

    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";

    private DiscoveryServiceConfiguration discoveryServiceConfiguration;
    private TlsConfiguration tlsConfiguration;
    private final static String COOKIE = "apimlAuthenticationToken";
    private String scheme;
    private String username;
    private String password;
    private String host;
    private int port;


    @Before
    public void setUp() {
        discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        tlsConfiguration = ConfigReader.environmentConfiguration().getTlsConfiguration();
        scheme = discoveryServiceConfiguration.getScheme();
        username = ConfigReader.environmentConfiguration().getCredentials().getUser();
        password = ConfigReader.environmentConfiguration().getCredentials().getPassword();
        host = discoveryServiceConfiguration.getHost();
        port = discoveryServiceConfiguration.getPort();
    }

    // /eureka endpoints
    @Test
    public void eurekaAppsWithCertIs200() throws Exception {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        given()
            .when()
            .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    @Test
    public void eurekaAppsWithoutAnythingIs403() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .when()
            .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
            .statusCode(is(HttpStatus.SC_FORBIDDEN))
            .header(HttpHeaders.WWW_AUTHENTICATE, nullValue());
    }

    @Test
    public void eurekaAppsWithBasicIs403() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .auth().basic(username, password)
            .when()
            .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
            .statusCode(is(HttpStatus.SC_FORBIDDEN));
    }

    // Gateway is discovered
    @Test
    public void eurekaAppsGatewayWithCertIs200() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        given()
            .when()
            .get(getDiscoveryUriWithPath("/eureka/apps/gateway"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    // /application health info

    @Test
    public void applicationInfoWithoutAnythingIs200() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .when()
            .get(getDiscoveryUriWithPath("/application/info"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }
    @Test
    public void applicationHealthWithoutAnythingIs200() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .when()
            .get(getDiscoveryUriWithPath("/application/health"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    // /application endpoints
    @Test
    public void applicationBeansWithoutAnythingIs401() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .when()
            .get(getDiscoveryUriWithPath("/application/beans"))
            .then()
            .statusCode(is(HttpStatus.SC_UNAUTHORIZED))
            .header(HttpHeaders.WWW_AUTHENTICATE, containsString(DISCOVERY_REALM));
    }

    @Test
    public void applicationBeansWithBasicIs200() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .auth().basic(username, password)
            .when()
            .get(getDiscoveryUriWithPath("/application/beans"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }
    @Test
    public void applicationBeansWithTokenIs200() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        String jwtToken = SecurityUtils.gatewayToken(username, password);
        given()
            .cookie(COOKIE, jwtToken)
            .when()
            .get(getDiscoveryUriWithPath("/application/beans"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    // /discovery endpoints
    @Test
    public void discoveryWithoutAnythingIs401() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .when()
            .get(getDiscoveryUriWithPath("/discovery/api/v1/staticApi"))
            .then()
            .statusCode(is(HttpStatus.SC_UNAUTHORIZED))
            .header(HttpHeaders.WWW_AUTHENTICATE, containsString(DISCOVERY_REALM));
    }

    @Test
    public void discoveryWithBasicIs200() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .auth().basic(username, password)
            .when()
            .get(getDiscoveryUriWithPath("/discovery/api/v1/staticApi"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    @Test
    public void discoveryWithTokenIs200() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        String jwtToken = SecurityUtils.gatewayToken(username, password);
        given()
            .cookie(COOKIE, jwtToken)
            .when()
            .get(getDiscoveryUriWithPath("/discovery/api/v1/staticApi"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    @Test
    public void discoveryWithCertIs200() throws Exception {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        given()
            .when()
            .get(getDiscoveryUriWithPath("/discovery/api/v1/staticApi"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    // root & ui
    @Test
    public void uiWithoutAnythingIs401() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .when()
            .get(getDiscoveryUriWithPath("/"))
            .then()
            .statusCode(is(HttpStatus.SC_UNAUTHORIZED))
            .header(HttpHeaders.WWW_AUTHENTICATE, containsString(DISCOVERY_REALM));
    }

    @Test
    public void uiWithBasicIs200() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .auth().basic(username, password)
            .when()
            .get(getDiscoveryUriWithPath("/"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    @Test
    public void uiWithTokenIs200() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        String jwtToken = SecurityUtils.gatewayToken(username, password);
        given()
            .cookie(COOKIE, jwtToken)
            .when()
            .get(getDiscoveryUriWithPath("/"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    @Test
    public void verifyHttpHeadersOnUi() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("X-Content-Type-Options","nosniff");
        expectedHeaders.put("X-XSS-Protection","1; mode=block");
        expectedHeaders.put("Cache-Control","no-cache, no-store, max-age=0, must-revalidate");
        expectedHeaders.put("Pragma","no-cache");
        expectedHeaders.put("Content-Type","text/html;charset=UTF-8");
        expectedHeaders.put("Transfer-Encoding","chunked");
        expectedHeaders.put("X-Frame-Options","DENY");

        List<String> forbiddenHeaders = new ArrayList<>();
        forbiddenHeaders.add("Strict-Transport-Security");

        Response response =  RestAssured
            .given()
            .auth().basic(username, password)
            .get(getDiscoveryUriWithPath("/"));
        Map<String,String> responseHeaders = new HashMap<>();
        response.getHeaders().forEach(h -> responseHeaders.put(h.getName(),h.getValue()));

        expectedHeaders.entrySet().forEach(h -> assertThat(responseHeaders, hasEntry(h.getKey(),h.getValue())));
        forbiddenHeaders.forEach(h -> assertThat(responseHeaders, not(hasKey(h))));
    }

    @Test
    public void verifyHttpHeadersOnApi() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("X-Content-Type-Options","nosniff");
        expectedHeaders.put("X-XSS-Protection","1; mode=block");
        expectedHeaders.put("Cache-Control","no-cache, no-store, max-age=0, must-revalidate");
        expectedHeaders.put("Pragma","no-cache");
        expectedHeaders.put("X-Frame-Options","DENY");

        List<String> forbiddenHeaders = new ArrayList<>();
        forbiddenHeaders.add("Strict-Transport-Security");

        Response response =  RestAssured
            .given()
            .auth().basic(username, password)
            .get(getDiscoveryUriWithPath("/application/info"));
        Map<String,String> responseHeaders = new HashMap<>();
        response.getHeaders().forEach(h -> responseHeaders.put(h.getName(),h.getValue()));

        expectedHeaders.entrySet().forEach(h -> assertThat(responseHeaders, hasEntry(h.getKey(),h.getValue())));
        forbiddenHeaders.forEach(h -> assertThat(responseHeaders, not(hasKey(h))));
    }

    @Test
    public void verifyHttpHeadersOnEureka() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("X-Content-Type-Options","nosniff");
        expectedHeaders.put("X-XSS-Protection","1; mode=block");
        expectedHeaders.put("Cache-Control","no-cache, no-store, max-age=0, must-revalidate");
        expectedHeaders.put("Pragma","no-cache");
        expectedHeaders.put("Content-Type","application/xml");
        expectedHeaders.put("X-Frame-Options","DENY");

        List<String> forbiddenHeaders = new ArrayList<>();
        forbiddenHeaders.add("Strict-Transport-Security");

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        Response response =  RestAssured
            .given()
            .get(getDiscoveryUriWithPath("/eureka/apps"));
        Map<String,String> responseHeaders = new HashMap<>();
        response.getHeaders().forEach(h -> responseHeaders.put(h.getName(),h.getValue()));

        expectedHeaders.entrySet().forEach(h -> assertThat(responseHeaders, hasEntry(h.getKey(),h.getValue())));
        forbiddenHeaders.forEach(h -> assertThat(responseHeaders, not(hasKey(h))));
    }

    @Test
    public void shouldSeeEurekaReplicasIfRegistered() throws Exception {
        final int instances = discoveryServiceConfiguration.getInstances();
        //@formatter:off
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        String xml =
            given()
                .auth().basic(username, password)
                .when()
                .get(getDiscoveryUriWithPath("/eureka/status"))
                .then()
                .statusCode(is(HttpStatus.SC_OK))
                .extract().body().asString();
        //@formatter:on

        xml = xml.replaceAll("com.netflix.eureka.util.StatusInfo", "StatusInfo");

        String availableReplicas = XmlPath.from(xml).getString("StatusInfo.applicationStats.available-replicas");
        String registeredReplicas = XmlPath.from(xml).getString("StatusInfo.applicationStats.registered-replicas");
        String unavailableReplicas = XmlPath.from(xml).getString("StatusInfo.applicationStats.unavailable-replicas");
        List<String> servicesList = Arrays.asList(registeredReplicas.split(","));
        if (instances == 1) {
            Assert.assertEquals("", registeredReplicas);
            Assert.assertEquals("", availableReplicas);
            Assert.assertEquals("", unavailableReplicas);
        } else {
            if (availableReplicas.charAt(availableReplicas.length() - 1) == ',') {
                availableReplicas = availableReplicas.substring(0, availableReplicas.length() - 1);
            }
            Assert.assertNotEquals("", registeredReplicas);
            Assert.assertNotEquals("", availableReplicas);
            Assert.assertEquals("", unavailableReplicas);
            Assert.assertEquals(registeredReplicas, availableReplicas);
            Assert.assertEquals(servicesList.size(), instances - 1);
        }
    }

    private SSLConfig getConfiguredSslConfig() {
        try {
            SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(
                    new File(tlsConfiguration.getKeyStore()),
                    getCharArray(tlsConfiguration.getKeyStorePassword()),
                    getCharArray(tlsConfiguration.getKeyPassword()),
                    (aliases, socket) -> tlsConfiguration.getKeyAlias())
                .loadTrustMaterial(
                    new File(tlsConfiguration.getTrustStore()),
                    getCharArray(tlsConfiguration.getTrustStorePassword()))
                .build();

            SSLSocketFactoryAdapter sslSocketFactory = new SSLSocketFactoryAdapter(new SSLConnectionSocketFactory(sslContext,
                NoopHostnameVerifier.INSTANCE));
            return SSLConfig.sslConfig().with().sslSocketFactory(sslSocketFactory);
        } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
            | CertificateException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private char[] getCharArray(String value) {
        return value != null ? value.toCharArray() : null;
    }

    private URI getDiscoveryUriWithPath(@Nonnull String path) throws Exception {
        return new URIBuilder()
            .setScheme(scheme)
            .setHost(host)
            .setPort(port)
            .setPath(path)
            .build();
    }
}
