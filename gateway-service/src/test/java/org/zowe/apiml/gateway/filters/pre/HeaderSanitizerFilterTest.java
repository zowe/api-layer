/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.filters.pre;

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class HeaderSanitizerFilterTest {
    private static final String PUBLIC_KEY = "X-Certificate-Public";
    private static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
    private static final String COMMON_NAME = "X-Certificate-CommonName";
    HeaderSanitizerFilter headerSanitizerFilter;
    private RequestContext context;

    @BeforeEach
    void setUp() {
        String[] headers = {PUBLIC_KEY, DISTINGUISHED_NAME, COMMON_NAME};
        headerSanitizerFilter = spy(new HeaderSanitizerFilter(headers));

        context = spy(new RequestContext());
        RequestContext.testSetCurrentContext(context);


    }

    @Test
    void whenHeaderPresent_thenNullsHeader() {
        context.addZuulRequestHeader(PUBLIC_KEY, "evil header");
        assertTrue(context.getZuulRequestHeaders().containsKey(PUBLIC_KEY.toLowerCase()));
        headerSanitizerFilter.run();
        verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, null);
        assertTrue(context.getZuulRequestHeaders().containsKey(PUBLIC_KEY.toLowerCase()));
        assertNull(context.getZuulRequestHeaders().get(PUBLIC_KEY.toLowerCase()));
    }

    @Test
    void whenHeaderNotPresent_thenDoesntDoAnything() {
        Arrays.stream(new String[] {PUBLIC_KEY, DISTINGUISHED_NAME, COMMON_NAME})
            .forEach(h -> assertFalse(context.getZuulRequestHeaders().containsKey(h.toLowerCase()), "Unexpected header in RequestContext: " + h));

        headerSanitizerFilter.run();

        Arrays.stream(new String[] {PUBLIC_KEY, DISTINGUISHED_NAME, COMMON_NAME})
            .forEach(h -> {
                assertFalse(context.getZuulRequestHeaders().containsKey(h.toLowerCase()), "Unexpected header in RequestContext: " + h);
                verify(context, times(0)).addZuulRequestHeader(PUBLIC_KEY, null);
            });
    }
}
