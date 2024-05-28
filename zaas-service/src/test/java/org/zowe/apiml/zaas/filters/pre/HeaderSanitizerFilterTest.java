/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.filters.pre;

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class HeaderSanitizerFilterTest {
    private static final String PUBLIC_KEY = "X-Certificate-Public";
    private static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
    private static final String COMMON_NAME = "X-Certificate-CommonName";
    HeaderSanitizerFilter headerSanitizerFilter;
    private RequestContext context;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        String[] headers = {PUBLIC_KEY, DISTINGUISHED_NAME, COMMON_NAME};
        headerSanitizerFilter = spy(new HeaderSanitizerFilter(headers));

        context = spy(new RequestContext());
        RequestContext.testSetCurrentContext(context);

        request = new MockHttpServletRequest();
        context.setRequest(request);
    }

    @Test
    void whenHeaderPresent_thenNullsHeader() {
        context.addZuulRequestHeader(PUBLIC_KEY, "evil header"); //this lowercases the header
        context.getZuulRequestHeaders().put(DISTINGUISHED_NAME, "evil header"); //this preserves case
        assertTrue(context.getZuulRequestHeaders().containsKey(PUBLIC_KEY.toLowerCase()));
        assertTrue(context.getZuulRequestHeaders().containsKey(DISTINGUISHED_NAME));

        headerSanitizerFilter.run();

        verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY.toLowerCase(), null);
        verify(context, times(1)).addZuulRequestHeader(DISTINGUISHED_NAME, null);

        assertTrue(context.getZuulRequestHeaders().containsKey(PUBLIC_KEY.toLowerCase())); //headers are lowercased when nulled
        assertTrue(context.getZuulRequestHeaders().containsKey(DISTINGUISHED_NAME.toLowerCase())); //headers are lowercased when nulled
        assertNull(context.getZuulRequestHeaders().get(PUBLIC_KEY.toLowerCase()));
        assertNull(context.getZuulRequestHeaders().get(DISTINGUISHED_NAME.toLowerCase()));
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

    @Test
    void whenOtherHeadersPresent_thenDoesntChangeThem() {
        Arrays.stream(new String[] {PUBLIC_KEY, DISTINGUISHED_NAME, COMMON_NAME})
            .forEach(h -> assertFalse(context.getZuulRequestHeaders().containsKey(h.toLowerCase()), "Unexpected header in RequestContext: " + h));
        context.getZuulRequestHeaders().put("SomeOtherHeader", "someValue");
        assertTrue(context.getZuulRequestHeaders().containsKey("SomeOtherHeader"));
        assertTrue(context.getZuulRequestHeaders().containsValue("someValue"));

        headerSanitizerFilter.run();

        assertTrue(context.getZuulRequestHeaders().containsKey("SomeOtherHeader"));
        assertTrue(context.getZuulRequestHeaders().containsValue("someValue"));
    }

    @Test
    void whenHeadersOnRequest_thenTakeThemIntoAccount() {
        // because headers that live on Request are not part of zuulRequestHeaders
        request.addHeader(DISTINGUISHED_NAME, "value");
        assertFalse(context.getZuulRequestHeaders().containsKey(DISTINGUISHED_NAME));
        assertFalse(context.getZuulRequestHeaders().containsKey(DISTINGUISHED_NAME.toLowerCase()));

        headerSanitizerFilter.run();

        assertTrue(context.getZuulRequestHeaders().containsKey(DISTINGUISHED_NAME.toLowerCase()));
        assertNull(context.getZuulRequestHeaders().get(DISTINGUISHED_NAME.toLowerCase()));
    }
}
