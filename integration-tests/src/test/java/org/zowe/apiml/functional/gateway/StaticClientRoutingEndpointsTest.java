/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.functional.gateway;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GatewayTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@GatewayTest
@TestsNotMeantForZowe
class StaticClientRoutingEndpointsTest implements TestWithStartedInstances {
    private static final String GREET = "/api/v1/discoverableclient/greeting";
    private static final String STATIC_GREET = "/api/v1/staticclient/greeting";

    @Test
    void whenStaticAndDynamicRoutedThenProducesSameResponse() throws Exception {

        final HttpResponse response = HttpRequestUtils.getResponse(GREET, SC_OK);
        final HttpResponse staticResponse = HttpRequestUtils.getResponse(STATIC_GREET, SC_OK);
        String regExp = "\"date\":\".+\",";
        String hello = EntityUtils.toString(response.getEntity()).replaceAll(regExp,"");
        String staticHello = EntityUtils.toString(staticResponse.getEntity()).replaceAll(regExp,"");

        assertEquals(response.getStatusLine().getStatusCode(), staticResponse.getStatusLine().getStatusCode());
        assertEquals(hello, staticHello);
    }
}
