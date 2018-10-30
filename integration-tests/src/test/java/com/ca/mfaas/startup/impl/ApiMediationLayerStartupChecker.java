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
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Checks and waits until the testing environment is ready to be tested.
 */
@Slf4j
public class ApiMediationLayerStartupChecker {
    private static final String CHECK_ENDPOINT = "/application/routes";
    private static final String[] ROUTES_TO_CHECK = new String[] {
        "/api/v1/apicatalog/**",
        "/api/v1/discoverableclient/**",
        "/api/v1/staticclient/**"
    };


    public ApiMediationLayerStartupChecker() { }

    /**
     * Waits x minutes until the discoverable-client and api-catalog services are started.
     */
    public void waitUntilReady() {
        await().atMost(3, MINUTES).pollInterval(10, SECONDS).until(this::isReady);
    }

    private boolean isReady() {
        return severalSuccessfulResponse(3);
    }

    private boolean severalSuccessfulResponse(int times) {
        for (int i = 0; i < times; i++) {
            if (!successfulResponse()) {
                return false;
            }
        }

        return true;
    }

    private boolean successfulResponse() {
        return checkServiceRouting();
    }

    private boolean checkServiceRouting() {
        log.info("Checking for API to be available for routing...");

        try {
            HttpGet request = HttpRequestUtils.getRequest(CHECK_ENDPOINT);
            final HttpResponse response = HttpClientUtils.client().execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.warn("Unexpected HTTP status code: {}", response.getStatusLine().getStatusCode());
                return false;
            }
            final String jsonResponse = EntityUtils.toString(response.getEntity());
            DocumentContext documentContext = JsonPath.parse(jsonResponse);
            for (String route : ROUTES_TO_CHECK) {
                if (documentContext.read(route) == null) {
                    log.warn("Route '{}' is not found", route);
                    return false;
                }
            }
            return true;
        } catch (IOException | PathNotFoundException e) {
            log.warn("Check failed: {}", e.getMessage());
        }
        return false;
    }
}
