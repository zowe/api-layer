/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EurekaBasicAuthEnvironmentPostProcessorTest {

    private final EurekaBasicAuthEnvironmentPostProcessor postProcessor = new EurekaBasicAuthEnvironmentPostProcessor();
    private MockEnvironment environment;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        environment.setProperty(EurekaBasicAuthEnvironmentPostProcessor.DEFAULT_ZONE_PROPERTY, "https://localhost:10011/eureka/");
        environment.setProperty(EurekaBasicAuthEnvironmentPostProcessor.EUREKA_USERID_PROPERTY, "eureka");
        environment.setProperty(EurekaBasicAuthEnvironmentPostProcessor.EUREKA_PASSWORD_PROPERTY, "password");
    }

    private String defaultZone() {
        return environment.getProperty(EurekaBasicAuthEnvironmentPostProcessor.DEFAULT_ZONE_PROPERTY);
    }

    @Nested
    class GivenVerificationDisabled {

        @BeforeEach
        void disableVerification() {
            environment.setProperty(EurekaBasicAuthEnvironmentPostProcessor.VERIFY_CERTIFICATES_PROPERTY, "false");
        }

        @Test
        void thenCredentialsAreEmbedded() {
            postProcessor.postProcessEnvironment(environment, null);
            assertEquals("https://eureka:password@localhost:10011/eureka/", defaultZone());
        }

        @Test
        void givenMultipleUrls_thenAllAreRewritten() {
            environment.setProperty(EurekaBasicAuthEnvironmentPostProcessor.DEFAULT_ZONE_PROPERTY,
                "https://host1:10011/eureka/,https://host2:10011/eureka/");
            postProcessor.postProcessEnvironment(environment, null);
            assertEquals(
                "https://eureka:password@host1:10011/eureka/,https://eureka:password@host2:10011/eureka/",
                defaultZone()
            );
        }

        @Test
        void givenUrlWithCredentials_thenItIsLeftUntouchedAndNoSourceAdded() {
            environment.setProperty(EurekaBasicAuthEnvironmentPostProcessor.DEFAULT_ZONE_PROPERTY,
                "https://eureka:password@localhost:10011/eureka/");
            postProcessor.postProcessEnvironment(environment, null);
            assertEquals("https://eureka:password@localhost:10011/eureka/", defaultZone());
            assertFalse(environment.getPropertySources().contains(EurekaBasicAuthEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
        }

        @Test
        void givenNoCredentials_thenDefaultZoneIsUnchanged() {
            environment.getPropertySources().remove("mockProperties");
            MockEnvironment noCreds = new MockEnvironment();
            noCreds.setProperty(EurekaBasicAuthEnvironmentPostProcessor.VERIFY_CERTIFICATES_PROPERTY, "false");
            noCreds.setProperty(EurekaBasicAuthEnvironmentPostProcessor.DEFAULT_ZONE_PROPERTY, "https://localhost:10011/eureka/");
            postProcessor.postProcessEnvironment(noCreds, null);
            assertEquals("https://localhost:10011/eureka/",
                noCreds.getProperty(EurekaBasicAuthEnvironmentPostProcessor.DEFAULT_ZONE_PROPERTY));
        }
    }

    @Nested
    class GivenVerificationEnabled {

        @Test
        void thenDefaultZoneIsUnchanged() {
            // verifySslCertificatesOfServices defaults to true
            postProcessor.postProcessEnvironment(environment, null);
            assertEquals("https://localhost:10011/eureka/", defaultZone());
            assertFalse(environment.getPropertySources().contains(EurekaBasicAuthEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
        }
    }
}
