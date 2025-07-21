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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.Credentials;
import org.zowe.apiml.util.config.DiscoverableClientConfiguration;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Checks and waits until the testing environment is ready to be tested.
 */
@Slf4j
public class ApiMediationLayerStartupChecker {

    private static final boolean IS_MODULITH_ENABLED = Boolean.parseBoolean(System.getProperty("environment.modulith"));

    private final GatewayServiceConfiguration gatewayConfiguration;
    private final DiscoverableClientConfiguration discoverableClientConfiguration;
    private final DiscoveryServiceConfiguration discoveryServiceConfiguration;
    private final Credentials credentials;
    private final List<Service> servicesToCheck = new ArrayList<>();
    private final String healthEndpoint = "/application/health";


    public ApiMediationLayerStartupChecker() {
        gatewayConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        credentials = ConfigReader.environmentConfiguration().getCredentials();
        discoverableClientConfiguration = ConfigReader.environmentConfiguration().getDiscoverableClientConfiguration();
        discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();

        servicesToCheck.add(new Service("Gateway", "$.status"));
        if (!IS_MODULITH_ENABLED) {
            servicesToCheck.add(new Service("ZAAS", "$.components.gateway.details.zaas"));
        }
        servicesToCheck.add(new Service("Api Catalog", "$.components.gateway.details.apicatalog"));
        servicesToCheck.add(new Service("Discovery Service", "$.components.gateway.details.discovery"));
    }

    public void waitUntilReady() {
        long poolInterval = 5;
        await()
            .atMost(10, MINUTES)
            .pollDelay(0, SECONDS)
            .pollInterval(poolInterval, SECONDS)
            .until(this::areAllServicesUp);
    }

    private DocumentContext getDocumentAsContext(HttpGet request) {
        try {
            final HttpResponse response = HttpClientUtils.client().execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.warn("Unexpected HTTP status code: {} for URI: {}. Message: {}", response.getStatusLine().getStatusCode(), request.getURI().toString(), EntityUtils.toString(response.getEntity()));
                return null;
            }
            final String jsonResponse = EntityUtils.toString(response.getEntity());
            log.debug("URI: {}, JsonResponse is {}", request.getURI().toString(), jsonResponse);

            if (StringUtils.isNotEmpty(jsonResponse)) {
                return JsonPath.parse(jsonResponse);
            }
            return null;
        } catch (IOException e) {
            log.warn("Check failed on getting the document: {}", e.getMessage());
            return null;
        }
    }

    private boolean areAllServicesUp() {
        try {
            HttpGet requestToGateway = HttpRequestUtils.getRequest(healthEndpoint);
            requestToGateway.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", credentials.getUser(), credentials.getPassword()).getBytes()));
            DocumentContext context = getDocumentAsContext(requestToGateway);
            if (context == null) {
                return false;
            }

            boolean areAllServicesUp = true;
            for (Service toCheck : servicesToCheck) {
                boolean isUp = isServiceUp(context, toCheck.path);
                logDebug(toCheck.name + " is {}", isUp);

                if (!isUp) {
                    areAllServicesUp = false;
                }
            }
            if (!IS_MODULITH_ENABLED && !isAuthUp()) {
                areAllServicesUp = false;
            }

            String allComponents = context.read("$.components.discoveryComposite.components.discoveryClient.details.services").toString();
            boolean isTestApplicationUp = allComponents.toLowerCase().contains("discoverableclient");
            boolean needsTestApplication = discoverableClientConfiguration.getInstances() > 0;

            log.debug("Discoverable Client is {}", isTestApplicationUp);
            log.debug("Needs Discoverable Client: {}", needsTestApplication);
            isTestApplicationUp = !needsTestApplication || isTestApplicationUp;


            Integer amountOfActiveGateways = context.read("$.components.gateway.details.gatewayCount");
            var expectedGatewayCount = Integer.getInteger("environment.gwCount", gatewayConfiguration.getInstances());

            boolean isValidAmountOfGatewaysUp = amountOfActiveGateways != null &&
                amountOfActiveGateways >= expectedGatewayCount;
            log.debug("There are {} gateways", amountOfActiveGateways);
            if (!isValidAmountOfGatewaysUp) {
                log.debug("Expecting at least {} gateways", gatewayConfiguration.getInstances());
                callEurekaApps();
                return false;
            }
            // Consider properly the case with multiple gateway services running on different ports.
            if (gatewayConfiguration.getInternalPorts() != null && !gatewayConfiguration.getInternalPorts().isEmpty()) {
                String[] internalPorts = gatewayConfiguration.getInternalPorts().split(",");
                String[] hosts = gatewayConfiguration.getHost().split(",");
                for (int i = 0; i < Math.min(internalPorts.length, hosts.length); i++) {
                    log.debug("Trying to access the Gateway at port {}", internalPorts[i]);
                    requestToGateway = HttpRequestUtils.getRequest(healthEndpoint);
                    requestToGateway.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", credentials.getUser(), credentials.getPassword()).getBytes()));
                    var response = HttpClientUtils.client().execute(requestToGateway);
                    if (response.getStatusLine().getStatusCode() != 200) {
                        log.debug("Response from gateway at {} was: {}", requestToGateway.getURI(), response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "undefined");
                        throw new IOException();
                    }
                }
            }

            var result = areAllServicesUp && isTestApplicationUp;
            if (!result) {
                log.debug("API ML is not ready, check which services are missing in the above messages");
            }
            return result;
        } catch (PathNotFoundException | IOException e) {
            log.warn("Check failed on retrieving the information from document: {}", e.getMessage());
            return false;
        }
    }

    private void callEurekaApps() {
        HttpGet requestToEurekaApps = new HttpGet(HttpRequestUtils.getUriFromService(discoveryServiceConfiguration, "/eureka/apps"));
        CloseableHttpClient client = HttpClients.custom().setSSLContext(SslContext.sslClientCertValid).build();
        try (client) {
            var response = client.execute(requestToEurekaApps);
            var entity = response.getEntity();
            if (entity != null) {
                log.debug("eureka/apps: {}", EntityUtils.toString(entity));
            } else {
                log.debug("eureka/apps entity is null");
            }
        } catch (Exception e) {
            log.error("Cannot call Eureka apps", e);
        }
    }

    private boolean isAuthUp() {
        HttpGet requestToZaas;
        if (!IS_MODULITH_ENABLED) {
            requestToZaas = new HttpGet(HttpRequestUtils.getUriFromZaas(healthEndpoint));
        } else {
            requestToZaas = new HttpGet(HttpRequestUtils.getUriFromGateway(healthEndpoint));
        }
        requestToZaas.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", credentials.getUser(), credentials.getPassword()).getBytes()));
        DocumentContext zaasContext = getDocumentAsContext(requestToZaas);
        if (zaasContext == null) {
            return false;
        }
        boolean isUp;
        if (!IS_MODULITH_ENABLED) {
            isUp = isServiceUp(zaasContext, "$.components.zaas.details.auth");
        } else {
            isUp = isServiceUp(zaasContext, "$.components.gateway.details.auth");
        }
        logDebug("Authentication Service is {}", isUp);
        return isUp;
    }

    private boolean isServiceUp(DocumentContext documentContext, String path) {
        return documentContext.read(path).equals("UP");
    }

    private void logDebug(String logMessage, boolean state) {
        log.debug(logMessage, state ? "UP" : "DOWN");
    }

    @AllArgsConstructor
    private class Service {
        String name;
        String path;
    }
}
