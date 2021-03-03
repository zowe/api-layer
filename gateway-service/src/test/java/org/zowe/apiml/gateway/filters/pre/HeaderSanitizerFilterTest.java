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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class HeaderSanitizerFilterTest {
    private static final String PUBLIC_KEY = "X-Certificate-Public";
    private static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
    private static final String COMMON_NAME = "X-Certificate-CommonName";
    HeaderSanitizerFilter headerSanitizerFilter;

    @Test
    void test() {
        String[] headers = {PUBLIC_KEY, DISTINGUISHED_NAME, COMMON_NAME};
        headerSanitizerFilter = spy(new HeaderSanitizerFilter(headers));
        RequestContext context = spy(RequestContext.class);
        RequestContext.testSetCurrentContext(context);
        headerSanitizerFilter.run();
        context.addZuulRequestHeader(PUBLIC_KEY, "evil header");
        verify(context, times(1)).addZuulRequestHeader(PUBLIC_KEY, null);
        assertNull(context.getZuulRequestHeaders().get(PUBLIC_KEY));
    }
}
