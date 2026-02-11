/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.proxy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.zowe.apiml.util.http.HttpRequestUtils;
import java.net.URI;
import org.zowe.apiml.util.requests.Endpoints;

import io.restassured.RestAssured;

import static io.restassured.RestAssured.given;

@Tag("UnknownHostnamesTest")
public class CertificateValidationDisabledTest {
    @Test
    void givenRequestToServiceWithInvalidHostname_thenRequestIsSuccessful() {
        RestAssured.useRelaxedHTTPSValidation();
        URI uri = HttpRequestUtils.getUriFromGateway(Endpoints.DISCOVERABLE_GREET);
        given()
            .log().ifValidationFails()
            .get(uri)
            .then()
            .log().ifValidationFails()
            .statusCode(200);
    }
}
