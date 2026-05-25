/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.config;

import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import jakarta.ws.rs.client.Client;
import org.apache.http.HttpStatus;
import org.apache.http.config.Registry;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.discovery.eureka.RefreshablePeerEurekaNodes;
import org.zowe.apiml.discovery.functional.DiscoveryFunctionalTest;

import java.io.IOException;
import java.lang.reflect.Field;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
class AttlsConfigTest {

    private String protocol = "http";

    @ActiveProfiles({ "attlsServer", "attlsClient" })
    @Nested
    class GivenAttlsModeEnabled extends DiscoveryFunctionalTest {

        @Autowired
        PeerEurekaNodes peerEurekaNodes;

        @Override
        protected String getProtocol() {
            return protocol;
        }

        @Test
        void whenAttlsClientEnabled_thenHttpSocketFactoryIsRegistered() throws Exception {
            assertInstanceOf(RefreshablePeerEurekaNodes.class, peerEurekaNodes, "The injected bean must be instance of RefreshablePeerEurekaNodes");

            var refreshableNodes = (RefreshablePeerEurekaNodes) peerEurekaNodes;
            var testPeerNode = refreshableNodes.createPeerEurekaNode("http://localhost:10011/eureka/");
            assertNotNull(testPeerNode, "The generated peer node must be not null");

            Client apacheClient = getClient(testPeerNode);
            assertNotNull(apacheClient, "The client Jersey must be not null");

            ClientConfig clientConfigObj = (ClientConfig) apacheClient.getConfiguration();
            var cm = (PoolingHttpClientConnectionManager) clientConfigObj.getProperty(ApacheClientProperties.CONNECTION_MANAGER);

            assertNotNull(cm);
            Field operatorField = PoolingHttpClientConnectionManager.class.getDeclaredField("connectionOperator");
            operatorField.setAccessible(true);
            Object connectionOperator = operatorField.get(cm);

            Field registryField = connectionOperator.getClass().getDeclaredField("socketFactoryRegistry");
            registryField.setAccessible(true);
            var registry = (Registry<?>) registryField.get(connectionOperator);

            assertNotNull(registry.lookup("http"));
            assertNotNull(registry.lookup("https"));
        }

        private static Client getClient(PeerEurekaNode testPeerNode) throws NoSuchFieldException, IllegalAccessException {
            var replicationClientField = testPeerNode.getClass().getSuperclass().getDeclaredField("replicationClient");
            replicationClientField.setAccessible(true);
            Object replicationClient = replicationClientField.get(testPeerNode);

            Field jerseyClientField;
            try {
                jerseyClientField = replicationClient.getClass().getDeclaredField("eurekaJerseyClient");
            } catch (NoSuchFieldException e) {
                jerseyClientField = replicationClient.getClass().getSuperclass().getDeclaredField("jerseyClient");
            }
            jerseyClientField.setAccessible(true);

            return (Client) jerseyClientField.get(replicationClient);
        }

        @Test
        void whenContextLoads_requestFailsWithHttps() {
            protocol = "https";
            assertThrows(IOException.class, () -> given()
                .log().all()
            .when()
                .get(getDiscoveryUriWithPath("/application/info"))
            .then()
                .log().all());
        }

        /**
         * This test verifies the call attempted to use AT-TLS filters
         */
        @Test
        void whenContextLoads_RequestFailsWithAttlsContextReason() {
            protocol = "http";
            given()
                .log().all()
            .when()
                .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
                .log().all()
                .statusCode(is(HttpStatus.SC_INTERNAL_SERVER_ERROR))
                .body(containsString("Connection is not secure."))
                .body(containsString("AttlsContextImpl.getStatConn"));
        }

    }

    /**
     * This test intends to verify ICSF workaround (no keyring load)
     */
    @Nested
    @TestPropertySource(
        properties = {
            "server.ssl.keyStoreType=",
            "server.ssl.keyStorePassword=",
            "server.ssl.keyPassword=",
            "server.ssl.keyAlias=",
            "server.ssl.keyStore="
        }
    )
    @ActiveProfiles({ "attlsServer", "attlsClient" })
    class GivenSslDisabled extends DiscoveryFunctionalTest {

        @Test
        void whenNoKeystore_thenStartupSuccess() {
            protocol = "http";
            given()
                .log().all()
            .when()
                .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
                .log().all()
                .statusCode(is(HttpStatus.SC_INTERNAL_SERVER_ERROR))
                .body(containsString("Connection is not secure."))
                .body(containsString("AttlsContextImpl.getStatConn"));
        }

    }

}
