/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.common;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

@AcceptanceTest
public class AcceptanceTestWithBasePath {
    protected String basePath;

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setBasePath() {
        basePath = String.format("https://localhost:%d", port);
        RestAssured.useRelaxedHTTPSValidation();
    }


}
