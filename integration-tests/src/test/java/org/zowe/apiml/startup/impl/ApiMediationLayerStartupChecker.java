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
import lombok.AllArgsConstructor;
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
import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Checks and waits until the testing environment is ready to be tested.
 */
@Slf4j
public class ApiMediationLayerStartupChecker {
    private final GatewayServiceConfiguration gatewayConfiguration;
    private final List<Service> servicesToCheck = new ArrayList<>();

    public ApiMediationLayerStartupChecker() {
        gatewayConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

        servicesToCheck.add(new Service("Gateway", "$.status"));
        servicesToCheck.add(new Service("Api Catalog", "$.details.gateway.details.apicatalog"));
        servicesToCheck.add(new Service("Discovery Service", "$.details.gateway.details.discovery"));
        servicesToCheck.add(new Service("Authentication Service", "$.details.gateway.details.auth"));
    }

    public void waitUntilReady() {
        long poolInterval = 10;
        if (gatewayConfiguration.getInstances() == 2) {
            poolInterval = 5;
        }
        await().atMost(10, MINUTES).pollDelay(0, SECONDS).pollInterval(poolInterval, SECONDS).until(this::isReady);
    }

    private boolean isReady() {
        log.info("Checking if the API Mediation Layer is ready to be used...");
        int times = 3;
        if (gatewayConfiguration.getInstances() == 2) {
            times = 2;
        }
        return severalSuccessfulResponses(times);
    }

    private boolean severalSuccessfulResponses(int times) {
        for (int i = 0; i < times; i++) {
            if (!areAllServicesUp()) {
                return false;
            }
        }

        return true;
    }

    private DocumentContext getDocumentAsContext() {
        try {
            HttpGet request = HttpRequestUtils.getRequest("/application/health");
            final HttpResponse response = HttpClientUtils.client().execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.warn("Unexpected HTTP status code: {}", response.getStatusLine().getStatusCode());
                return null;
            }
            final String jsonResponse = EntityUtils.toString(response.getEntity());
            log.debug("URI: {}, JsonResponse is {}", request.getURI().toString(), jsonResponse);

            return JsonPath.parse(jsonResponse);
        } catch (IOException e) {
            log.warn("Check failed on getting the document: {}", e.getMessage());
            return null;
        }
    }

    private boolean areAllServicesUp() {
        try {
            DocumentContext context = getDocumentAsContext();
            if (context == null) {
                return false;
            }

            boolean areAllServicesUp = true;
            for(Service toCheck: servicesToCheck) {
                boolean isUp = isServiceUp(context, toCheck.path);
                logDebug(toCheck.name + " is {}", isUp);

                if(!isUp) {
                    areAllServicesUp = false;
                }
            }

            boolean isTestApplicationUp = context.read("$.details.discoveryComposite.details.discoveryClient.details.services").toString()
                .contains("discoverableclient");
            logDebug("Discoverable Client is {}", isTestApplicationUp);

            Integer amountOfActiveGateways = context.read("$.details.gateway.details.gatewayCount");
            boolean isValidAmountOfGatewaysUp = amountOfActiveGateways != null &&
                amountOfActiveGateways.equals(gatewayConfiguration.getInstances());
            log.debug("There is {} gateways", amountOfActiveGateways);

            return areAllServicesUp &&
                isValidAmountOfGatewaysUp &&
                isTestApplicationUp;
        } catch (PathNotFoundException e) {
            log.warn("Check failed on retrieving the information from document: {}", e.getMessage());
            return false;
        }
    }

    private boolean isServiceUp(DocumentContext documentContext, String path) {
        return documentContext.read(path).equals("UP");
    }

    private void logDebug(String logMessage, boolean state) {
        log.debug(logMessage, state ? "UP": "DOWN");
    }

    @AllArgsConstructor
    private class Service {
        String name;
        String path;
    }
}
