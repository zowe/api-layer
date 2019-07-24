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

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
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
    private final GatewayServiceConfiguration gatewayConfiguration;

    public ApiMediationLayerStartupChecker() {
        gatewayConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    }

    public void waitUntilReady() {
        await().atMost(3, MINUTES).pollDelay(0, SECONDS).pollInterval(10, SECONDS).until(this::isReady);
    }

    private boolean isReady() {
        log.info("Checking of the API Mediation Layer is ready to be used...");
        return severalSuccessfulResponses(3);
    }

    private boolean severalSuccessfulResponses(int times) {
        for (int i = 0; i < times; i++) {
            if (!successfulResponse()) {
                return false;
            }
        }

        return true;
    }

    private boolean successfulResponse() {
        try {
            HttpGet request = HttpRequestUtils.getRequest("/application/health");
            final HttpResponse response = HttpClientUtils.client().execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.warn("Unexpected HTTP status code: {}", response.getStatusLine().getStatusCode());
                return false;
            }
            final String jsonResponse = EntityUtils.toString(response.getEntity());
            DocumentContext documentContext = JsonPath.parse(jsonResponse);
            return documentContext.read("$.status").equals("UP") && allInstancesUp(documentContext)
                && testApplicationUp(documentContext);
        } catch (IOException | PathNotFoundException e) {
            log.warn("Check failed: {}", e.getMessage());
        }
        return false;
    }

    private boolean testApplicationUp(DocumentContext documentContext) {
        return documentContext.read("$.details.discoveryComposite.details.discoveryClient.details.services").toString()
            .contains("discoverableclient");
    }

    private boolean allInstancesUp(DocumentContext documentContext) {
        return documentContext.read("$.details.gateway.details.apicatalog").equals("UP")
            && documentContext.read("$.details.gateway.details.discovery").equals("UP")
            && documentContext.read("$.details.gateway.details.auth").equals("UP")
            && documentContext.read("$.details.gateway.details.gatewayCount")
            .equals(gatewayConfiguration.getInstances());
    }
}
