/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.startup.impl;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

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
        long poolInterval = 10;
        if (gatewayConfiguration.getInstances() == 2) {
            poolInterval = 5;
        }
        await().atMost(10, MINUTES).pollDelay(0, SECONDS).pollInterval(poolInterval, SECONDS).until(this::isReady);
    }

    private boolean isReady() {
        log.info("Checking of the API Mediation Layer is ready to be used...");
        int times = 3;
        if (gatewayConfiguration.getInstances() == 2) {
            times = 2;
        }
        return severalSuccessfulResponses(times);
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
            log.debug("URI: {}, JsonResponse is {}", request.getURI().toString(), jsonResponse);

            DocumentContext documentContext = JsonPath.parse(jsonResponse);

            boolean isGatewayUp = documentContext.read("$.status").equals("UP");
            log.debug("Gateway is {}", isGatewayUp ? "UP" : "DOWN");

            boolean isAllInstancesUp = allInstancesUp(documentContext);
            log.debug("All instances are {}", isAllInstancesUp ? "UP" : "DOWN");

            boolean isTestApplicationUp = testApplicationUp(documentContext);
            log.debug("Discoverableclient is {}", isTestApplicationUp ? "UP" : "DOWN");

            return isGatewayUp && isAllInstancesUp && isTestApplicationUp;
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
