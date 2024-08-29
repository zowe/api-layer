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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ForbidEncodedSlashesFilterFactoryTest {

    private static final String ENCODED_REQUEST_URI = "/api/v1/encoded%2fslash";
    private static final String NORMAL_REQUEST_URI = "/api/v1/normal";

    @Nested
    @TestPropertySource(properties = "apiml.service.allowEncodedSlashes=false")
    class Responses {

        @Autowired
        ForbidEncodedSlashesFilterFactory filter;

        @Test
        void givenNormalRequestUri_whenFilterApply_thenSuccess() {
            MockServerHttpRequest request = MockServerHttpRequest
                .get(NORMAL_REQUEST_URI)
                .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            var response = filter.apply("").filter(exchange, e -> {
                exchange.getResponse().setRawStatusCode(200);
                return Mono.empty();
            });
            response.block();
            assertTrue(exchange.getResponse().getStatusCode().is2xxSuccessful());
        }

        @Test
        void givenRequestUriWithEncodedSlashes_whenFilterApply_thenReturnBadRequest() throws URISyntaxException {
            MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, new URI(ENCODED_REQUEST_URI))
                .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            assertThrows(ForbidSlashException.class, () -> filter.apply("").filter(exchange, e -> Mono.empty()).block());
        }

    }

}
