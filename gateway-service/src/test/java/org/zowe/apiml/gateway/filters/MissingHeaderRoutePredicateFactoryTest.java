/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class MissingHeaderRoutePredicateFactoryTest {

    private Predicate<ServerWebExchange> predicate;

    @BeforeEach
    void setup() {
        MissingHeaderRoutePredicateFactory.Config config = new MissingHeaderRoutePredicateFactory.Config();
        config.setHeader("myheader");
        predicate = new MissingHeaderRoutePredicateFactory().apply(config);
    }

    @ParameterizedTest(name = "When header {0} is set to {1}, the value after processing should be {2}")
    @CsvSource({
        "myheader,GW,false",
        "myheader,,false",
        "otherheader,othervalue,true",
        ",,true",
    })
    void testPredicate(String headerName, String headerValue, boolean result) {
        MockServerHttpRequest request;
        if (headerName != null) {
            request = MockServerHttpRequest
                .get("/")
                .header(headerName, headerValue).build();
        } else {
            request = MockServerHttpRequest
                .get("/").build();
        }
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        assertThat(predicate.test(exchange)).isEqualTo(result);
    }

    @Test
    void testToStringFormat() {
        assertThat(predicate.toString()).contains("Missing header: myheader");
    }
}
