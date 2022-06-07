/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.authentication.pat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.categories.InfinispanStorageTest;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.Endpoints;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

@InfinispanStorageTest
public class AccessTokenServiceTest {

    public static final URI REVOKE_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.REVOKE_ACCESS_TOKEN);
    public static final URI VALIDATE_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.VALIDATE_ACCESS_TOKEN);

    @BeforeAll
    static void setup() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenValidJWT_invalidateTheToken(){
        String jwt = SecurityUtils.gatewayToken();
        Map<String, String> bodyContent = new HashMap<>();
        bodyContent.put("token", jwt);
        given().contentType(ContentType.JSON).body(bodyContent).when()
            .delete(REVOKE_ENDPOINT)
            .then().statusCode(200);
        given().contentType(ContentType.JSON).body(bodyContent).when()
            .post(VALIDATE_ENDPOINT)
            .then().statusCode(401);
    }
}
