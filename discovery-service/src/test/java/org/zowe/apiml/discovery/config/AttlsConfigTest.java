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
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.discovery.eureka.RefreshablePeerEurekaNodes;
import org.zowe.apiml.discovery.functional.DiscoveryFunctionalTest;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(Lifecycle.PER_CLASS)
class AttlsConfigTest {

    private String protocol = "http";

    @ActiveProfiles({ "attlsServer", "attlsClient" })
    @Nested
    class GivenAttlsModeEnabled extends DiscoveryFunctionalTest {

        @Autowired
        private PeerEurekaNodes peerEurekaNodes;

        @Override
        protected String getProtocol() {
            return protocol;
        }

        @Test
        void whenAttlsClientEnabled_thenHttpSocketFactoryIsRegistered() {
            assertInstanceOf(RefreshablePeerEurekaNodes.class, peerEurekaNodes, "The injected bean must be instance of RefreshablePeerEurekaNodes");

            var refreshableNodes = (RefreshablePeerEurekaNodes) peerEurekaNodes;
            var testPeerNode = refreshableNodes.createPeerEurekaNode("http://localhost:10011/eureka/");

            Client apacheClient = getClient(testPeerNode);

            ClientConfig clientConfigObj = (ClientConfig) apacheClient.getConfiguration();
            var cm = (PoolingHttpClientConnectionManager) clientConfigObj.getProperty(ApacheClientProperties.CONNECTION_MANAGER);

            Object connectionOperator = ReflectionTestUtils.getField(cm, "connectionOperator");

            assertNotNull(connectionOperator);
            var registry = (Registry<?>) ReflectionTestUtils.getField(connectionOperator, "socketFactoryRegistry");
            assertNotNull(registry);

            assertNotNull(registry.lookup("http"));
            assertNotNull(registry.lookup("https"));
        }

        private static Client getClient(PeerEurekaNode testPeerNode) {
            Object replicationClient = ReflectionTestUtils.getField(testPeerNode, "replicationClient");
            assertNotNull(replicationClient);

            return (Client) ReflectionTestUtils.getField(replicationClient, "jerseyClient");
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
