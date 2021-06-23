/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.ha;

import io.restassured.RestAssured;
import io.restassured.path.xml.XmlPath;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.HATest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Verify that eureka is aware of other replicas if they are online.
 */
@HATest
class EurekaReplicationTest implements TestWithStartedInstances {
    private DiscoveryServiceConfiguration discoveryServiceConfiguration;
    private String username;
    private String password;
    private String[] hosts = discoveryServiceConfiguration.getHost().split(",");

    @BeforeEach
    void setUp() {
        discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        username = ConfigReader.environmentConfiguration().getCredentials().getUser();
        password = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    }

    /**
     * Based on configuration it tests either for one or for more instances.
     */
    @Nested
    class GivenMultipleEurekaInstances {
        @Nested
        class WhenLookingForEurekas {
            @Test
            void eurekaReplicasAreVisible() throws Exception {
                final int instances = discoveryServiceConfiguration.getInstances();
                //@formatter:off
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
                String xml =
                    given()
                        .auth().basic(username, password)
                    .when()
                        .get(HttpRequestUtils.getUriFromDiscovery("/eureka/status", hosts[0]))
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
                    assertThat(registeredReplicas, is(""));
                    assertThat(availableReplicas, is(""));
                    assertThat(unavailableReplicas, is(""));
                } else {
                    if (availableReplicas.charAt(availableReplicas.length() - 1) == ',') {
                        availableReplicas = availableReplicas.substring(0, availableReplicas.length() - 1);
                    }
                    assertThat(registeredReplicas, is(not("")));
                    assertThat(availableReplicas, is(not("")));
                    assertThat(unavailableReplicas, is(""));

                    assertThat(registeredReplicas, is(availableReplicas));
                    assertThat(servicesList, hasSize(instances - 1));
                }
            }
        }
    }
}
