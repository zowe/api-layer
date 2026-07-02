/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EurekaClientConfigurationTest {

    private ApiMediationServiceConfig config;
    private Ssl ssl;

    @BeforeEach
    void setUp() {
        ssl = new Ssl();
        config = ApiMediationServiceConfig.builder()
            .discoveryServiceUrls(Collections.singletonList("https://localhost:10011/eureka/"))
            .discoveryUserid("eureka")
            .discoveryPassword("password".toCharArray())
            .ssl(ssl)
            .build();
    }

    private List<String> serviceUrls() {
        return new EurekaClientConfiguration(config).getEurekaServerServiceUrls("default");
    }

    @Nested
    class GivenSslVerificationEnabled {

        @Test
        void thenCredentialsAreNotEmbedded() {
            ssl.setVerifySslCertificatesOfServices(true);
            assertEquals(Collections.singletonList("https://localhost:10011/eureka/"), serviceUrls());
        }
    }

    @Nested
    class GivenSslVerificationDisabled {

        @BeforeEach
        void disableVerification() {
            ssl.setVerifySslCertificatesOfServices(false);
        }

        @Test
        void thenCredentialsAreEmbedded() {
            assertEquals(Collections.singletonList("https://eureka:password@localhost:10011/eureka/"), serviceUrls());
        }

        @Test
        void givenNoCredentials_thenUrlIsUnchanged() {
            config.setDiscoveryUserid(null);
            config.setDiscoveryPassword(null);
            assertEquals(Collections.singletonList("https://localhost:10011/eureka/"), serviceUrls());
        }
    }

    @Nested
    class GivenNoSslConfiguration {

        @Test
        void thenCredentialsAreNotEmbedded() {
            config.setSsl(null);
            assertEquals(Collections.singletonList("https://localhost:10011/eureka/"), serviceUrls());
        }
    }
}
