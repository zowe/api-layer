/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discoveryservice.staticdef;

import com.ca.mfaas.utils.http.HttpRequestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

public class StaticClientEndpointsTest {
    private static final String GREET = "/api/v1/discoverableclient/greeting";
    private static final String STATIC_GREET = "/api/v1/staticclient/greeting";

    @Test
    public void shouldSameResponseAsDynamicService() throws Exception {
        // When
        final HttpResponse response = HttpRequestUtils.getResponse(GREET, SC_OK);
        final HttpResponse staticResponse = HttpRequestUtils.getResponse(STATIC_GREET, SC_OK);
        String regExp = "\"date\":\".+\",";
        String hello = EntityUtils.toString(response.getEntity()).replaceAll(regExp, "");
        String staticHello = EntityUtils.toString(staticResponse.getEntity()).replaceAll(regExp, "");

        // Then
        assertEquals(response.getStatusLine().getStatusCode(), staticResponse.getStatusLine().getStatusCode());
        assertEquals(hello, staticHello);
    }
}
