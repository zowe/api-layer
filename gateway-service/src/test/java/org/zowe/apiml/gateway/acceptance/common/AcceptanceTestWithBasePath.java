/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance.common;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.zowe.apiml.product.web.HttpConfig;

@AcceptanceTest
public class AcceptanceTestWithBasePath {

    protected String basePath;

    @LocalServerPort
    protected int port;

    @Autowired
    HttpConfig httpConfig;

    @BeforeEach
    void setBasePath() {
        basePath = String.format("https://localhost:%d", port);
        RestAssured.config = RestAssured.config.sslConfig(new SSLConfig().sslSocketFactory(
            new SSLSocketFactory(httpConfig.getSecureSslContextWithoutKeystore(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
        ));
    }

}
