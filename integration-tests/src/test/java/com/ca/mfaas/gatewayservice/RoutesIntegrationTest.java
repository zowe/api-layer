/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.gatewayservice;

import com.ca.mfaas.utils.http.HttpRequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;


import static org.apache.http.HttpStatus.SC_OK;

@Slf4j
public class RoutesIntegrationTest {

    private final static String APPLICATION_ROUTES_ENDPOINT = "/application/routes";

    @Test
    public void shouldNotContainSuperfluousEndpoints()  throws Exception{
        final HttpResponse response = HttpRequestUtils.getResponse(APPLICATION_ROUTES_ENDPOINT, SC_OK);
        final String jsonResponse = EntityUtils.toString(response.getEntity());
        Assert.assertEquals(false, jsonResponse.toLowerCase().contains("\"/app/"));
        Assert.assertEquals(false, jsonResponse.toLowerCase().contains("\"/gateway/**"));
        Assert.assertEquals(false, jsonResponse.toLowerCase().contains("\"/staticclient/**"));
        Assert.assertEquals(false, jsonResponse.toLowerCase().contains("\"/zosmfca32/**"));
        Assert.assertEquals(false, jsonResponse.toLowerCase().contains("\"/apicatalog/**"));
        Assert.assertEquals(false, jsonResponse.toLowerCase().contains("\"/discoverableclient/**"));
        Assert.assertEquals(false, jsonResponse.toLowerCase().contains("\"/cadbmds01/**"));
        Assert.assertEquals(false, jsonResponse.toLowerCase().contains("\"/discovery/**"));
    }
}
