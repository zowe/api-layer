/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.filters;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderRouteStepFilterFactoryTest {

    @ParameterizedTest(name = "When header {1} is set to {2} and the filter works with header {0} the value after processing should be {3}")
    @CsvSource({
        "another,header-name,some-value,some-value",
        "header-name,header-name,GW,",
        "header-name,header-name,GW/,",
        "header-name,header-name,GW/x,x",
        "header-name,header-name,GW/x/y,x/y",
    })
    void test(String headerNameConfig, String headerName, String headerValueOrigin, String headerValueNew) {
        HeaderRouteStepFilterFactory.Config config = new HeaderRouteStepFilterFactory.Config();
        config.setHeader(headerNameConfig);
        HeaderRouteStepFilterFactory headerRouteStepFilterFactory = new HeaderRouteStepFilterFactory();
        GatewayFilter filter = headerRouteStepFilterFactory.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest
            .get("/")
            .header(headerName, headerValueOrigin).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, exchange2 -> {
            assertEquals(headerValueNew, exchange2.getRequest().getHeaders().getFirst(headerName));
            return null;
        });
    }

}
