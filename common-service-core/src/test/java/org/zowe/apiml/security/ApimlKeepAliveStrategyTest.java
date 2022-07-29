/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHeaderIterator;
import org.apache.http.protocol.HTTP;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApimlKeepAliveStrategyTest {

    @Test
    void givenMultipleKeepAliveHeaders_thenOnlyTimeoutValueIsReturned() {
        HttpResponse response = mock(HttpResponse.class);
        Header header = new BasicHeader(HTTP.CONN_KEEP_ALIVE, "timeout=10");
        Header header1 = new BasicHeader(HTTP.CONN_KEEP_ALIVE, "20");
        Header[] headers = {header,header1};
        HeaderIterator headerIterator = new BasicHeaderIterator(headers, HTTP.CONN_KEEP_ALIVE);
        when(response.headerIterator(HTTP.CONN_KEEP_ALIVE)).thenReturn(headerIterator);
        ApimlKeepAliveStrategy strategy = ApimlKeepAliveStrategy.INSTANCE;
        assertEquals(10_000L, strategy.getKeepAliveDuration(response, null));
    }

    @Test
    void givenMissingKeepAliveHeader_thenReturnDefaultTimeout() {
        HttpResponse response = mock(HttpResponse.class);
        Header header = new BasicHeader(HTTP.CONTENT_TYPE, "text/html");
        Header[] headers = {header};
        HeaderIterator headerIterator = new BasicHeaderIterator(headers, HTTP.CONTENT_TYPE);
        when(response.headerIterator(HTTP.CONN_KEEP_ALIVE)).thenReturn(headerIterator);
        ApimlKeepAliveStrategy strategy = ApimlKeepAliveStrategy.INSTANCE;
        assertEquals(2_000L, strategy.getKeepAliveDuration(response, null));
    }
}
