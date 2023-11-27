/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple Spring Context test to verify attls filter chain setup is in place with the right properties being sent
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "server.internal.ssl.enabled=false",
        "server.attls.enabled=true",
        "server.ssl.enabled=false",
        "server.service.scheme=http"
    },
    classes = NewSecurityConfiguration.class
)
@DirtiesContext
@ActiveProfiles("GatewayAttlsConfigTest")
@Disabled
public class AttlsConfigTest {

    @Autowired
    HttpSecurity http;

    @Nested
    class GivenAttlsModeEnabled {
        // verify spring context loads
        // verify the security filter chain is complete with the attls filter in place.

        @Test
        void test() {

            // load the configuration and test that a request to a controller fails with a http 500 due to at-tls
            // context not being available?

        }
    }

    @RestController
    @Profile("GatewayAttlsConfigTest")
    class TestController {

    }

    @Configuration
    @Profile("GatewayAttlsConfigTest")
    class TestConfiguration {

        @Bean
        public ObjectMapper mapper() {
            return new ObjectMapper();
        }

    }

}
