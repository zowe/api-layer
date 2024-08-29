/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance.common;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.zowe.apiml.acceptance.requests.SecurityRequests;

@AcceptanceTest
public class AcceptanceTestWithBasePath {
    protected String basePath;
    protected SecurityRequests securityRequests;

    @LocalServerPort
    protected int port;

    @BeforeEach
    public void setBasePath() {
        basePath = String.format("https://localhost:%d", port);
        securityRequests = new SecurityRequests(basePath);
        RestAssured.useRelaxedHTTPSValidation();
    }
}
