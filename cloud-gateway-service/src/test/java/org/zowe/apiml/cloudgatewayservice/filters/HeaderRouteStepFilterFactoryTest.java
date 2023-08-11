/*
 * Copyright (c) 2022 Broadcom.  All Rights Reserved.  The term
 * "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This software and all information contained therein is
 * confidential and proprietary and shall not be duplicated,
 * used, disclosed, or disseminated in any way except as
 * authorized by the applicable license agreement, without the
 * express written permission of Broadcom.  All authorized
 * reproductions must be marked with this language.
 *
 * EXCEPT AS SET FORTH IN THE APPLICABLE LICENSE AGREEMENT, TO
 * THE EXTENT PERMITTED BY APPLICABLE LAW, BROADCOM PROVIDES THIS
 * SOFTWARE WITHOUT WARRANTY OF ANY KIND, INCLUDING WITHOUT
 * LIMITATION, ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT WILL BROADCOM
 * BE LIABLE TO THE END USER OR ANY THIRD PARTY FOR ANY LOSS OR
 * DAMAGE, DIRECT OR INDIRECT, FROM THE USE OF THIS SOFTWARE,
 * INCLUDING WITHOUT LIMITATION, LOST PROFITS, BUSINESS
 * INTERRUPTION, GOODWILL, OR LOST DATA, EVEN IF BROADCOM IS
 * EXPRESSLY ADVISED OF SUCH LOSS OR DAMAGE.
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