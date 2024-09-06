/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.functional;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.zowe.apiml.apicatalog.ApiCatalogApplication;
import org.zowe.apiml.product.web.HttpConfig;

@SpringBootTest(
    classes = ApiCatalogApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration
public abstract class ApiCatalogFunctionalTest {

    @LocalServerPort
    protected int port;

    @Autowired
    private HttpConfig httpConfig;

    @Value("${apiml.service.hostname:localhost}")
    protected String hostname;

    @BeforeEach
    void setUp() {
        RestAssured.config = RestAssured.config.sslConfig(new SSLConfig().sslSocketFactory(
            new SSLSocketFactory(httpConfig.getSecureSslContextWithoutKeystore(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
        ));
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected String getCatalogUriWithPath(String path) {
        return getCatalogUriWithPath("https", path);
    }

    protected String getCatalogUriWithPath(String scheme, String path) {
        return String.format("%s://%s:%d/%s", scheme, hostname, port, path);
    }
}
