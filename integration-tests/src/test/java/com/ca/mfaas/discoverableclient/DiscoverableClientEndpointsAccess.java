/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discoverableclient;

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import com.ca.mfaas.utils.http.HttpRequestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

public class DiscoverableClientEndpointsAccess {
    private static final String GATEWAY_URL = "/api/v1/discoverableclient/instance/gateway-url";

    private GatewayServiceConfiguration serviceConfiguration;

    @Before
    public void setUp() {
        serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    }

    @Test
    public void shouldGetGatewayUrl() throws Exception {
        // When
        HttpResponse response = HttpRequestUtils.getResponse(GATEWAY_URL, SC_OK);
        final String jsonResponse = EntityUtils.toString(response.getEntity());

        String expected = new URIBuilder()
            .setScheme(serviceConfiguration.getScheme())
            .setHost(serviceConfiguration.getHost())
            .setPort(serviceConfiguration.getPort()).build().toString();

        // Then
        assertEquals(expected.toLowerCase(), jsonResponse.toLowerCase());
    }

}
