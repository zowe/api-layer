/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.gateway;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_GREET;

@DiscoverableClientDependentTest
@Tag("CloudGatewayServiceRouting")
public class CloudGatewayRoutingTest implements TestWithStartedInstances {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";
    static CloudGatewayConfiguration conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();

    @ParameterizedTest(name = "When header X-Forward-To is set to {1} should return 200")
    @CsvSource({
        "apiml1" + DISCOVERABLE_GREET,
        "cloud-gateway" + DISCOVERABLE_GREET,
        DISCOVERABLE_GREET,
    })
    void testRoutingWithBasePath(String basePath) throws URISyntaxException {
        RestAssured.useRelaxedHTTPSValidation();

        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath);
        given().get(new URI(scgUrl)).then().statusCode(200);
    }

    @ParameterizedTest(name = "When header X-Forward-To is set to {1} should return 200")
    @CsvSource({
        "apiml1",
        "cloud-gateway/discoverableclient",
        "discoverableclient",
    })
    void testRoutingWithHeader(String forwardTo) throws URISyntaxException {
        RestAssured.useRelaxedHTTPSValidation();

        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), DISCOVERABLE_GREET);
        given().header(HEADER_X_FORWARD_TO, forwardTo)
            .get(new URI(scgUrl)).then().statusCode(200);
    }
}
