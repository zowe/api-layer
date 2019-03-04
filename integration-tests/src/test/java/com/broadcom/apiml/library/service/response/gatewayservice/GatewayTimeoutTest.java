/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.response.gatewayservice;

import com.broadcom.apiml.library.service.response.utils.categories.SlowTests;
import com.broadcom.apiml.library.service.response.utils.http.HttpClientUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.broadcom.apiml.library.service.response.utils.http.HttpRequestUtils.getUriFromGateway;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class GatewayTimeoutTest {
    private static final String API_V1_GREETING_URI = "/api/v1/discoverableclient/greeting";
    private static final int DEFAULT_TIMEOUT = 30000;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GatewayTimeoutTest.class);

    @Test
    @Category(SlowTests.class)
    @SuppressWarnings("squid:S1160")
    public void shouldCallLongButBelowTimeoutRequest() throws IOException {
        // Given
        HttpGet request = createGreetingRequestWithDelay(DEFAULT_TIMEOUT - 1000);

        // When
        final HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
    }

    @Test
    @Category(SlowTests.class)
    @SuppressWarnings("squid:S1160")
    public void shouldTimeoutRequestWithGatewayTimeoutHttpResponseCode() throws IOException {
        // Given
        HttpGet request = createGreetingRequestWithDelay(DEFAULT_TIMEOUT + 1000);

        // When
        final HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_GATEWAY_TIMEOUT));
    }

    private HttpGet createGreetingRequestWithDelay(int delay) {
        try {
            URI uri = new URIBuilder(getUriFromGateway(API_V1_GREETING_URI)).setParameter("delayMs", Integer.toString(delay)).build();
            return new HttpGet(uri);
        } catch (URISyntaxException e) {
            log.error("Test failed", e);
            fail(e.getMessage());
            return null;
        }
    }
}
