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
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.util.config.*;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Checks and waits until the testing environment is ready to be tested.
 */
@Slf4j
public class ApiMediationLayerStartupChecker {

    private static final boolean IS_MODULITH_ENABLED = Boolean.parseBoolean(System.getProperty("environment.modulith"));

    private final String authorizationHeader;
    private final List<Service> servicesToCheck = new ArrayList<>();
    private final String healthEndpoint = "/application/health";

    public ApiMediationLayerStartupChecker() {
        var credentials = ConfigReader.environmentConfiguration().getCredentials();
        authorizationHeader = "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", credentials.getUser(), credentials.getPassword()).getBytes());

        servicesToCheck.add(new Service("Gateway", "$.status", ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()));
        if (!IS_MODULITH_ENABLED) {
            servicesToCheck.add(new Service("ZAAS", "$.components.gateway.details.zaas", ConfigReader.environmentConfiguration().getZaasConfiguration()));
        }
        servicesToCheck.add(new Service("Api Catalog", "$.components.gateway.details.apicatalog", ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration()));
        servicesToCheck.add(new Service("Discovery Service", "$.components.gateway.details.discovery", ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration()));
    }

    public void waitUntilReady() {
        long poolInterval = 5;
        await()
            .atMost(10, MINUTES)
            .pollDelay(0, SECONDS)
            .pollInterval(poolInterval, SECONDS)
            .until(this::isApimlReady);
    }

    private static DocumentContext getDocumentAsContext(HttpGet request) {
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

    private boolean isApimlReady() {
        try {
            return Stream.of(
                checkHealthEndpointWithEureka(ConfigReader.environmentConfiguration().getZaasConfiguration()),
                checkHealthEndpointWithEureka(ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()),
                // Consider properly the case with multiple gateway services running on different ports.
                callInternalPorts(),
                allServicesAreUp()
            ).allMatch(x -> x);
        } catch (Exception e) {
            log.error("Error during checking if APIML is up", e);
            return false;
        }
    }

    private String getHealthEndpoint(ServiceConfiguration serviceConfiguration, String host) {
        String path = healthEndpoint;
        if (serviceConfiguration instanceof ApiCatalogServiceConfiguration) {
            path = "/apicatalog" + path;
        }
        return String.format("%s://%s:%d%s", serviceConfiguration.getScheme(), host, serviceConfiguration.getPort(), path);
    }

    private boolean allServicesAreUp() {
        return servicesToCheck.stream()
            .map(service -> service.configuration)
            .filter(service -> !IS_MODULITH_ENABLED || !isModulithComponent(service.getServiceId()))
            .flatMap(service -> Arrays.stream(service.getHost().split(","))
                .map(host -> getHealthEndpoint(service, host))
                .map(HttpGet::new)
                .map(request -> {
                    request.addHeader("Authorization", authorizationHeader);
                    DocumentContext context = getDocumentAsContext(request);
                    return (context != null) && "UP".equals(context.read("$.status"));
                })
            ).allMatch(x -> x);
    }

    private boolean checkHealthEndpointWithEureka(ServiceConfiguration serviceConfiguration) {
        if (serviceConfiguration == null) {
            return true;
        }
        return Arrays.stream(serviceConfiguration.getHost().split(","))
            .map(host -> getHealthEndpoint(serviceConfiguration, host))
            .map(HttpGet::new)
            .allMatch(request -> {
                request.addHeader("Authorization", authorizationHeader);
                DocumentContext context = getDocumentAsContext(request);

                return isApimlReadyByFullHealthEndpoint(context, request.getURI().getHost());
            });
    }

    private boolean isApimlReadyByFullHealthEndpoint(DocumentContext context, String host) {
        try {
            if (context == null) {
                return false;
            }

            JSONArray servicesJsonArray = context.read("$.components.discoveryComposite.components.discoveryClient.details.services");
            List<String> services = servicesJsonArray.stream().map(Objects::toString).map(String::toLowerCase).toList();

            boolean areAllServicesUp = true;
            for (Service toCheck : servicesToCheck) {
                if (toCheck.configuration instanceof GatewayServiceConfiguration) {
                    boolean isUp = isServiceUp(context, toCheck.path);
                    logDebug(toCheck.name + " is {}", isUp);
                    areAllServicesUp &= isUp;
                }
                areAllServicesUp &= services.contains(toCheck.configuration.getServiceId().toLowerCase());
                areAllServicesUp &= checkServicesCount(context, toCheck.configuration, host);
            }
            if (!IS_MODULITH_ENABLED && !isAuthUp()) {
                areAllServicesUp = false;
            }


            if (!areAllServicesUp) {
                log.debug("API ML is not ready, check which services are missing in the above messages");
            }

            return areAllServicesUp;
        } catch (PathNotFoundException e) {
            log.warn("Check failed on retrieving the information from document: {}", e.getMessage());
            return false;
        }
    }

    private boolean isModulithComponent(String serviceId) {
        return StringUtils.equalsAnyIgnoreCase(serviceId,
            CoreService.API_CATALOG.getServiceId(),
            CoreService.CACHING.getServiceId(),
            CoreService.ZAAS.getServiceId(),
            CoreService.DISCOVERY.getServiceId()
        );
    }

    private <V> V getService(Map<String, V> map, String key) {
        V out = map.get(key.toLowerCase());
        if (out == null) {
            out = map.get(key.toUpperCase());
        }
        if (out == null) {
            out = map.entrySet().stream()
                .filter(e -> key.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Cannot find record for " + key));
        }
        return out;
    }

    private boolean checkServicesCount(DocumentContext context, ServiceConfiguration serviceConfiguration, String gatewayHost) {
        if (serviceConfiguration.getInstances() == 0) {
            return true;
        }

        Map<String, Integer> services = context.read("$.components.discoveryComposite.components.eureka.details.applications");
        Integer amountOfActiveService = getService(services, serviceConfiguration.getServiceId());
        if (amountOfActiveService == null) {
            amountOfActiveService = services.get(serviceConfiguration.getServiceId().toUpperCase());
        }
        var expectedCount = serviceConfiguration.getInstances();
        if (serviceConfiguration instanceof GatewayServiceConfiguration) {
            expectedCount = Integer.getInteger("environment.gwCount", expectedCount);
        }

        if (IS_MODULITH_ENABLED && isModulithComponent(serviceConfiguration.getServiceId())) {
            expectedCount = Math.min(expectedCount, 1);
        }

        boolean isValidAmountOfServicesUp = amountOfActiveService != null &&
            amountOfActiveService >= expectedCount;
        log.debug("There are {} {} in GW on {}", amountOfActiveService, serviceConfiguration.getServiceId(), gatewayHost);

        if (!isValidAmountOfServicesUp) {
            log.debug("Expecting at least {} services ({})", expectedCount, serviceConfiguration.getServiceId());
            callEurekaApps();
            return false;
        }

        return true;
    }

    private boolean callInternalPorts() {
        var gatewayConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

        if (StringUtils.isBlank(gatewayConfiguration.getInternalPorts())) {
            log.debug("No internal ports are defined");
            return true;
        }

        String[] internalPorts = gatewayConfiguration.getInternalPorts().split(",");
        String[] hosts = gatewayConfiguration.getHost().split(",");

        for (int i = 0; i < Math.min(internalPorts.length, hosts.length); i++) {
            log.debug("Trying to access the Gateway at port {}", internalPorts[i]);
            var requestToGateway = HttpRequestUtils.getRequest(healthEndpoint);
            requestToGateway.addHeader("Authorization", authorizationHeader);
            try {
                var response = HttpClientUtils.client().execute(requestToGateway);

                if (response.getStatusLine().getStatusCode() != 200) {
                    log.debug("Response from gateway at {} was: {}", requestToGateway.getURI(), response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "undefined");
                    return false;
                }
            } catch (IOException ioException) {
                return false;
            }
        }
        return true;
    }

    private void callEurekaApps() {
        var discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        HttpGet requestToEurekaApps = new HttpGet(HttpRequestUtils.getUriFromService(discoveryServiceConfiguration, "/eureka/apps"));
        CloseableHttpClient client = HttpClients.custom()
            .setSSLContext(SslContext.sslClientCertValid)
            .setSSLHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
            .build();
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
        requestToZaas.addHeader("Authorization", authorizationHeader);
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
        ServiceConfiguration configuration;

    }

}
