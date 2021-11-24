/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.metrics.functional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.zowe.apiml.metrics.MetricsServiceApplication;

import io.restassured.RestAssured;

@SpringBootTest(classes = MetricsServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.ssl.keyStore=../keystore/localhost/localhost.keystore.p12",
        "server.ssl.trustStore=../keystore/localhost/localhost.truststore.p12" })
public abstract class MetricsFunctionalTest {
    @LocalServerPort
    protected int port;

    @Value("${apiml.service.hostname:localhost}")
    protected String hostname;

    @BeforeEach
    void setUp() throws Exception {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected String getDiscoveryUriWithPath(String path) {
        return String.format("https://%s:%d", hostname, port) + path;
    }

}
