/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.functional;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.zowe.apiml.discovery.DiscoveryServiceApplication;

@SpringBootTest(
    classes = DiscoveryServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@DirtiesContext
public abstract class DiscoveryFunctionalTest {

    protected static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";
    @LocalServerPort
    protected int port;

    @Value("${apiml.service.hostname:localhost}")
    protected String hostname;

    @BeforeEach
    void setUp() throws Exception {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected String getProtocol() {
        return "http";
    }

    protected String getDiscoveryUriWithPath(String path) {
        return String.format("%s://%s:%d", getProtocol(), hostname, port) + path;
    }
}
