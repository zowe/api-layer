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
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;

public class CloudGatewayProxyTest {

    @Test
    @Tag("CloudGatewayProxyTest")
    void givenRequestHeader_thenRouteToProvidedHost() throws URISyntaxException {
        CloudGatewayConfiguration conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();
        String scgUrl = String.format("%s://%s:%s/%s", conf.getScheme(), conf.getHost(), conf.getPort(), "gateway/version");
        given().header("X-Request-Id", "gatewaygateway-service")
            .get(new URI(scgUrl)).then().statusCode(200);
        given().header("X-Request-Id", "gatewaygateway-service-2")
            .get(new URI(scgUrl)).then().statusCode(200);
    }
}
