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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OtelServiceFilterFactoryTest {

    private static final String SERVICE_ID = "myServiceId";
    private static final String INSTANCE_ID = "myService:instanceId:1";

    @Test
    void givenConfiguredFilter_whenApply_thenSetServiceAndInstanceId() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/aPath").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        var config = new OtelServiceFilterFactory.Config();
        config.setServiceId(SERVICE_ID);
        config.setInstanceId(INSTANCE_ID);

        new OtelServiceFilterFactory().apply(config).filter(exchange, e -> Mono.empty().then());

        var attributes = ((AttributesBuilder) ReflectionTestUtils.getField(OtelRequestContext.of(exchange), "attributesBuilder")).build();
        assertEquals(SERVICE_ID.toLowerCase(), attributes.get(AttributeKey.stringKey("service.id")));
        assertEquals(INSTANCE_ID.toLowerCase(), attributes.get(AttributeKey.stringKey("service.instance.id")));
    }

}
