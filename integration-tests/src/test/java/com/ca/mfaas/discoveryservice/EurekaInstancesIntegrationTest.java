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

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.DiscoveryServiceConfiguration;
import com.ca.mfaas.utils.config.TlsConfiguration;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.path.xml.XmlPath;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

public class EurekaInstancesIntegrationTest {
    private static final String EUREKA_STATUS = "/eureka/status";
    private DiscoveryServiceConfiguration discoveryServiceConfiguration;
    private TlsConfiguration tlsConfiguration;

    @Before
    public void setUp() {
        discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        tlsConfiguration = ConfigReader.environmentConfiguration().getTlsConfiguration();
    }

    @Test
    public void shouldSeeEurekaReplicasIfRegistered() throws Exception {
        final String scheme = discoveryServiceConfiguration.getScheme();
        final String username = discoveryServiceConfiguration.getUser();
        final String password = discoveryServiceConfiguration.getPassword();
        final String host = discoveryServiceConfiguration.getHost();
        final int port = discoveryServiceConfiguration.getPort();
        final int instances = discoveryServiceConfiguration.getInstances();
        URI uri = new URIBuilder().setScheme(scheme).setHost(host).setPort(port).setPath(EUREKA_STATUS).build();

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        String xml =
            given()
                .auth().basic(username, password)
            .when()
                .get(uri)
            .then()
                .statusCode(is(200))
                .extract().body().asString();

        xml = xml.replaceAll("com.netflix.eureka.util.StatusInfo", "StatusInfo");

        String availableReplicas = XmlPath.from(xml).getString("StatusInfo.applicationStats.available-replicas");
        String registeredReplicas = XmlPath.from(xml).getString("StatusInfo.applicationStats.registered-replicas");
        String unavailableReplicas = XmlPath.from(xml).getString("StatusInfo.applicationStats.unavailable-replicas");
        List<String> servicesList = Arrays.asList(registeredReplicas.split(","));
        if (instances == 1 ) {
            Assert.assertEquals("", registeredReplicas);
            Assert.assertEquals("", availableReplicas);
            Assert.assertEquals("", unavailableReplicas);
        } else {
            if (availableReplicas.charAt(availableReplicas.length() - 1) == ',') {
                availableReplicas = availableReplicas.substring(0, availableReplicas.length() - 1);
            }
            Assert.assertNotEquals("", registeredReplicas);
            Assert.assertNotEquals("",availableReplicas);
            Assert.assertEquals("", unavailableReplicas);
            Assert.assertEquals(registeredReplicas, availableReplicas);
            Assert.assertEquals(servicesList.size(), instances - 1);
        }
    }

    private SSLConfig getConfiguredSslConfig() {
        try {
            String[] protocols = new String[] { tlsConfiguration.getProtocol() };
            String[] ciphers = tlsConfiguration.getCiphers().toArray(new String[0]);
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

            SSLSocketFactory sslSocketFactory = new SSLSocketFactory(sslContext,
                SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            return SSLConfig.sslConfig().with().sslSocketFactory(sslSocketFactory);
        } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
                | CertificateException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private char[] getCharArray(String value) {
        return value != null ? value.toCharArray() : null;
    }

}
