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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.zowe.apiml.util.config.*;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks and waits until the testing environment is ready to be tested.
 */
@Slf4j
public class ApiMediationLayerStartupChecker {

    private static final boolean IS_MODULITH_ENABLED = Boolean.parseBoolean(System.getProperty("environment.modulith"));

    private static final long POOL_INTERVAL = 5;

    private final GatewayServiceConfiguration gatewayConfiguration;
    private final DiscoverableClientConfiguration discoverableClientConfiguration;
    private final DiscoveryServiceConfiguration discoveryServiceConfiguration;
    private final ApiCatalogServiceConfiguration apiCatalogServiceConfiguration;
    private final CachingServiceConfiguration cachingServiceConfiguration;
    private final Credentials credentials;
    private final String credentialsHeader;
    private final List<Service> servicesToCheck = new ArrayList<>();
    private final List<Instance> instancesToCheck = new ArrayList<>();
    private final String healthEndpoint = "/application/health";

    private int minimumEurekaVersion;

    public ApiMediationLayerStartupChecker() {
        gatewayConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        credentials = ConfigReader.environmentConfiguration().getCredentials();
        credentialsHeader = "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", credentials.getUser(), credentials.getPassword()).getBytes());
        discoverableClientConfiguration = ConfigReader.environmentConfiguration().getDiscoverableClientConfiguration();
        discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        apiCatalogServiceConfiguration = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration();
        cachingServiceConfiguration = ConfigReader.environmentConfiguration().getCachingServiceConfiguration();

        servicesToCheck.add(new Service("Gateway", "$.status"));
        if (!IS_MODULITH_ENABLED) {
            servicesToCheck.add(new Service("ZAAS", "$.components.gateway.details.zaas"));
        }
        servicesToCheck.add(new Service("Api Catalog", "$.components.gateway.details.apicatalog"));
        servicesToCheck.add(new Service("Discovery Service", "$.components.gateway.details.discovery"));

        if (!IS_MODULITH_ENABLED) {
            // these services are not registered on all sides, and it is not necessary to check (GW check is enough)
            instancesToCheck.addAll(Instance.of(discoveryServiceConfiguration));
            instancesToCheck.addAll(Instance.of(apiCatalogServiceConfiguration, "apicatalog.instances"));
        }
        instancesToCheck.addAll(Instance.of(gatewayConfiguration, "gateway.instances"));
        instancesToCheck.addAll(Instance.of(discoverableClientConfiguration, "discoverableclient.instances"));
        instancesToCheck.addAll(Instance.of(cachingServiceConfiguration, "caching.instances"));
    }

    void initSsl() {
        if (SslContext.sslClientCertValid == null) {
            TlsConfiguration tlsCfg = ConfigReader.environmentConfiguration().getTlsConfiguration();
            SslContextConfigurer sslContextConfigurer = new SslContextConfigurer(tlsCfg.getKeyStorePassword(), tlsCfg.getClientKeystore(), tlsCfg.getKeyStore());
            try {
                SslContext.prepareSslAuthentication(sslContextConfigurer);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    void awaitFor(Callable<Boolean> check, int durationMin) {
        await()
            .atMost(durationMin, MINUTES)
            .pollDelay(0, SECONDS)
            .pollInterval(POOL_INTERVAL, SECONDS)
            .until(check);
    }

    public void waitUntilReady() {
        initSsl();

        awaitFor(this::areAllInstancesOnboarded, 2);
        this.minimumEurekaVersion = getEurekaVersion(Instance.of(discoveryServiceConfiguration).get(0));
        assertTrue(this.minimumEurekaVersion >= 0, "Cannot obtain eurekaVersion from Discovery service");
        awaitFor(this::areAllInstancesRegistryUpToDate, 1);
        awaitFor(this::areAllServicesUp, 1);
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

    private boolean areAllInstanceOnInEureka(DocumentContext documentContext) {
        Set<String> onboarded = ((JSONArray) documentContext.read("applications.application.*.instance.*.instanceId")).stream()
            .map(String.class::cast)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        Set<String> expectedInstanceIds = instancesToCheck.stream()
            .map(Instance::getInstanceId)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        List<String> missing = expectedInstanceIds.stream().filter(id -> !onboarded.contains(id)).sorted().toList();
        if (missing.isEmpty()) {
            return true;
        }

        log.debug("{} services has not onboarded yet: {}", missing.size(), StringUtils.join(missing, ", "));
        return false;
    }

    private boolean areAllInstancesOnboarded() {
        HttpGet requestToEurekaApps = new HttpGet(HttpRequestUtils.getUriFromService(discoveryServiceConfiguration, "/eureka/apps"));
        requestToEurekaApps.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
        try (CloseableHttpClient client = HttpClients.custom().setSSLContext(SslContext.sslClientCertValid).build()) {
            var response = client.execute(requestToEurekaApps);
            var entity = response.getEntity();
            if (entity != null) {
                String entityString = EntityUtils.toString(entity);
                log.debug("eureka/apps: {}", entityString);
                return areAllInstanceOnInEureka(JsonPath.parse(entityString));
            } else {
                log.debug("eureka/apps entity is null");
            }
        } catch (Exception e) {
            log.error("Cannot call Eureka apps", e);
        }
        return false;
    }

    private int getEurekaVersion(Instance instance) {
        HttpGet requestToEurekaApps = new HttpGet(instance.getEurekaVersionUrl());
        requestToEurekaApps.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
        if (instance.serviceConfiguration.isBasicSupported()) {
            requestToEurekaApps.addHeader(HttpHeaders.AUTHORIZATION, credentialsHeader);
        }
        try (CloseableHttpClient client = HttpClients.custom().setSSLContext(SslContext.sslClientCertValid).build()) {
            var doc = JsonPath.parse(EntityUtils.toString(client.execute(requestToEurekaApps).getEntity()));
            return doc.read("version");
        } catch (Exception e) {
            log.debug("Eurekaversion endpoint is on accessible on " + instance.getInstanceId(), e);
        }
        return -1;
    }

    private boolean areAllInstancesRegistryUpToDate() {
        List<String> notUpdated = new ArrayList<>();
        for (Instance instance : instancesToCheck) {
            int version = getEurekaVersion(instance);
            if (version < this.minimumEurekaVersion) {
                notUpdated.add(instance.getInstanceId());
            }
        }
        if (notUpdated.isEmpty()) {
            return true;
        }

        log.debug("There are instances that has not been updated yet: {}", StringUtils.join(notUpdated, ", "));
        return false;
    }

    private boolean areAllServicesUp() {
        try {
            var gatewayHosts = gatewayConfiguration.getHost().split(",");
            var requestToGateway1 = HttpRequestUtils.getRequest(gatewayHosts[0], healthEndpoint);
            // If second one does not exist, redundant call and check to same gateway
            var requestToGateway2 = HttpRequestUtils.getRequest(gatewayHosts.length > 1 ? gatewayHosts[1] : gatewayHosts[0], healthEndpoint);

            requestToGateway1.addHeader("Authorization", credentialsHeader);
            requestToGateway2.addHeader("Authorization", credentialsHeader);
            DocumentContext context1 = getDocumentAsContext(requestToGateway1);
            DocumentContext context2 = getDocumentAsContext(requestToGateway2);

            if (context1 == null || context2 == null) {
                return false;
            }

            boolean areAllServicesUp = true;
            for (Service toCheck : servicesToCheck) {
                boolean isUp = isServiceUp(context1, toCheck.path);
                logDebug(toCheck.name + " is {}", isUp);

                if (!isUp) {
                    areAllServicesUp = false;
                }
            }
            if (!IS_MODULITH_ENABLED && !isAuthUp()) {
                areAllServicesUp = false;
            }

            String allComponents = context1.read("$.components.discoveryComposite.components.discoveryClient.details.services").toString();
            boolean isTestApplicationUp = allComponents.toLowerCase().contains("discoverableclient");
            boolean needsTestApplication = discoverableClientConfiguration.getInstances() > 0;

            log.debug("Discoverable Client is {}", isTestApplicationUp);
            log.debug("Needs Discoverable Client: {}", needsTestApplication);
            isTestApplicationUp = !needsTestApplication || isTestApplicationUp;

            Integer amountOfActiveGateways1 = context1.read("$.components.gateway.details.gatewayCount");
            Integer amountOfActiveGateways2 = context2.read("$.components.gateway.details.gatewayCount");
            var expectedGatewayCount = Integer.getInteger("environment.gwCount", gatewayConfiguration.getInstances());

            boolean isValidAmountOfGatewaysUp = amountOfActiveGateways1 != null && amountOfActiveGateways2 != null &&
                amountOfActiveGateways1 >= expectedGatewayCount && amountOfActiveGateways2 >= expectedGatewayCount;
            log.debug("There are {} gateways in GW1 and {} in GW2", amountOfActiveGateways1, amountOfActiveGateways2);

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
                    requestToGateway1 = HttpRequestUtils.getRequest(healthEndpoint);
                    requestToGateway1.addHeader("Authorization", credentialsHeader);
                    var response = HttpClientUtils.client().execute(requestToGateway1);

                    if (response.getStatusLine().getStatusCode() != 200) {
                        log.debug("Response from gateway at {} was: {}", requestToGateway1.getURI(), response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "undefined");
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
            requestToZaas = new HttpGet(HttpRequestUtils.getUriFromZaas(healthEndpoint).get());
        } else {
            requestToZaas = new HttpGet(HttpRequestUtils.getUriFromGateway(healthEndpoint));
        }
        requestToZaas.addHeader("Authorization", credentialsHeader);
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

    @Data
    private static class Instance {

        private final String hostname;
        private final String serviceId;
        private final int port;
        private final ServiceConfiguration serviceConfiguration;

        Instance(String hostname, String serviceId, int port, ServiceConfiguration serviceConfiguration) {
            this.hostname = hostname;
            this.serviceId = serviceId;
            this.port = port;
            this.serviceConfiguration = serviceConfiguration;
        }

        static List<Instance> of(ServiceConfiguration serviceConfiguration) {
            return Arrays.stream(
                Optional.ofNullable(serviceConfiguration)
                    .map(ServiceConfiguration::getHost)
                    .orElse("")
                    .split("[,;]")
                )
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(String::toLowerCase)
                .map(host -> new Instance(host, serviceConfiguration.getServiceId(), serviceConfiguration.getPort(), serviceConfiguration))
                .toList();
        }

        static List<Instance> of(ServiceConfiguration serviceConfiguration, String countProperty) {
            List<Instance> allInstances = of(serviceConfiguration);
            String countString = System.getProperty(countProperty);
            if (StringUtils.isNotBlank(countString)) {
                int count = Integer.parseInt(countString);
                if ((count >= 0) && (count <= allInstances.size())) {
                    return allInstances.subList(0, count);
                }
                log.warn("Invalid count of services: {}", countString);
            }
            return allInstances;
        }

        String getUrl(String basePath) {
            return new DefaultUriBuilderFactory().builder()
                .scheme("https")
                .host(this.hostname)
                .port(this.port)
                .path(this.serviceConfiguration.getServletContext() + basePath)
                .toUriString();
        }

        public String getHealthEndpointUrl() {
            return getUrl("application/health");
        }

        public String getEurekaVersionUrl() {
            return getUrl("application/eurekaversion");
        }

        public String getInstanceId() {
            return (this.hostname + ":" + this.serviceId + ":" + this.port).toLowerCase();
        }

    }

}
