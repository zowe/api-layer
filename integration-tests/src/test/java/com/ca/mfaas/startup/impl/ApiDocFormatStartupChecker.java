/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.startup.impl;

import com.ca.mfaas.utils.http.HttpClientUtils;
import com.ca.mfaas.utils.http.HttpRequestUtils;
import com.ca.mfaas.utils.http.HttpSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
public class ApiDocFormatStartupChecker {

    public ApiDocFormatStartupChecker() { }

    /**
     * Waits x minutes until the catalog swagger endpoints is in correct format.
     */
    public void waitUntilReady() {
        await().atMost(3, MINUTES).pollInterval(10, SECONDS).until(this::isReady);
    }

    private boolean isReady() {
        boolean result = false;
        String getApiCatalogApiDocEndpoint = "/api/v1/apicatalog/apidoc/apicatalog/v1";
        String jsonResponse = "";
        try {
            HttpResponse response = getResponse(getApiCatalogApiDocEndpoint);
            jsonResponse = EntityUtils.toString(response.getEntity());
        } catch (Exception ignore) { }

        if (jsonResponse.contains("/api/v1/apicatalog/containers")) {
            result = true;
        } else {
            log.info("Did not find correct endpoints");
            log.info("API Doc for API Catalog is: " + jsonResponse);
        }

        return result;
    }

    private HttpResponse getResponse(String endpoint) throws IOException {
        String cookie = HttpSecurityUtils.getCookieForApiCatalog();
        HttpGet request = HttpRequestUtils.getRequest(endpoint);
        request = (HttpGet) HttpSecurityUtils.addCookie(request, cookie);

        HttpResponse response = HttpClientUtils.client().execute(request);

        return response;
    }
}
