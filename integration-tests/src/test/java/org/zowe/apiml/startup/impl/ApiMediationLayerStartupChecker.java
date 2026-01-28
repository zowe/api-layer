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
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.util.config.*;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Checks and waits until the testing environment is ready to be tested.
 */
@Slf4j
public class ApiMediationLayerStartupChecker {

    private static final long POOL_INTERVAL = 5;

    private static final boolean IS_MODULITH_ENABLED = Boolean.parseBoolean(System.getProperty("environment.modulith"));
    private static final Credentials CREDENTIALS = ConfigReader.environmentConfiguration().getCredentials();
    private static final String CREDENTIALS_HEADER = "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", CREDENTIALS.getUser(), CREDENTIALS.getPassword()).getBytes());
    private static final String APPLICATION_JSON_HEADER = HttpHeaderValues.APPLICATION_JSON.toString();

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

    public void waitUntilReady() {
        initSsl();
        ApimlInstance.load().forEach(ApimlInstance::waitUntilReady);
    }

    @Data
    private static class Instance {

        private final String scheme;
        private final String hostname;
        private final String serviceId;
        private final int port;
        private final ServiceConfiguration serviceConfiguration;

        private Instance(String hostname, ServiceConfiguration serviceConfiguration) {
            this.scheme = serviceConfiguration.getScheme();
            this.hostname = hostname;
            this.serviceId = serviceConfiguration.getServiceId();
            this.port = serviceConfiguration.getPort();
            this.serviceConfiguration = serviceConfiguration;
        }

        private static List<String> getAllHosts(ServiceConfiguration serviceConfiguration) {
            List<String> hosts = new ArrayList<>();
            if (serviceConfiguration == null) {
                return hosts;
            }
            if (StringUtils.isNotBlank(serviceConfiguration.getHost())) {
                hosts.addAll(Arrays.asList(serviceConfiguration.getHost().split("[,;]")));
            }
            if (serviceConfiguration instanceof DiscoveryServiceConfiguration discoveryServiceConfiguration) {
                String additionalHost = discoveryServiceConfiguration.getAdditionalHost();
                if (StringUtils.isNotBlank(additionalHost)) {
                    hosts.addAll(Arrays.asList(additionalHost.split("[,;]")));
                }
            }
            return hosts;
        }

        private static List<Instance> of(ServiceConfiguration serviceConfiguration) {
            return getAllHosts(serviceConfiguration).stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(String::toLowerCase)
                .map(host -> new Instance(host, serviceConfiguration))
                .toList();
        }

        private static List<Instance> of(ServiceConfiguration serviceConfiguration, String countProperty) {
            List<Instance> allInstances = of(serviceConfiguration);
            String countString = System.getProperty(countProperty);
            if (StringUtils.isNotBlank(countString)) {
                try {
                    int count = Integer.parseInt(countString);
                    if (count >= 0 && count <= allInstances.size()) {
                        return allInstances.subList(0, count);
                    }
                    log.warn("Invalid count of services {}: {}", serviceConfiguration.getServiceId(), countString);
                } catch (NumberFormatException e) {
                    log.warn("Invalid service instances value: {} -> {}", countProperty, countString);
                }
            }
            return allInstances;
        }

        private String getUrl(String basePath) {
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
            String prefix = this.serviceConfiguration.isStaticallyRegistred() ? "STATIC-" : "";
            return prefix + (this.hostname + ":" + this.serviceId + ":" + this.port).toLowerCase();
        }

    }

    private static class ApimlInstance {

        private int minimumEurekaVersion = -1;

        private final List<Instance> allInstances;
        private final List<Instance> registryCheckInstances;
        private final List<Instance> registryVersionCheckInstances;

        private ApimlInstance(Predicate<Instance> instanceMatcher) {
            var config = ConfigReader.environmentConfiguration();

            var instances = new ArrayList<Instance>();
            instances.addAll(Instance.of(config.getDiscoveryServiceConfiguration(), "discovery.instances"));
            instances.addAll(Instance.of(config.getApiCatalogServiceConfiguration(), "apicatalog.instances"));
            instances.addAll(Instance.of(config.getGatewayServiceConfiguration(), "gateway.instances"));
            instances.addAll(Instance.of(config.getDiscoverableClientConfiguration(), "discoverableclient.instances"));
            instances.addAll(Instance.of(config.getCachingServiceConfiguration(), "caching.instances"));
            instances.addAll(Instance.of(config.getZosmfServiceConfiguration(), "zosmf.instances"));
            instances.addAll(Instance.of(config.getZaasConfiguration(), "zaas.instances"));
            allInstances = instances.stream().filter(instanceMatcher).collect(Collectors.toList());

            if (IS_MODULITH_ENABLED) {
                // these services are not registered on all sides, and it is not necessary to check (GW check is enough)
                registryVersionCheckInstances = this.without(
                    CoreService.API_CATALOG.getServiceId(), CoreService.DISCOVERY.getServiceId(),
                    CoreService.CACHING.getServiceId(), config.getZosmfServiceConfiguration().getServiceId()
                );
                registryCheckInstances = this.without(CoreService.API_CATALOG, CoreService.DISCOVERY);
            } else {
                registryVersionCheckInstances = this.without(config.getZosmfServiceConfiguration().getServiceId());
                registryCheckInstances = allInstances;
            }
        }

        private void awaitFor(Callable<Boolean> check, int durationMin) {
            await()
                .atMost(durationMin, MINUTES)
                .pollDelay(0, SECONDS)
                .pollInterval(POOL_INTERVAL, SECONDS)
                .until(check);
        }

        private DocumentContext getDocumentAsContext(URI uri, boolean basicAuth) {
            HttpGet request = new HttpGet(uri);
            request.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON_HEADER);
            if (basicAuth) {
                request.addHeader(HttpHeaders.AUTHORIZATION, CREDENTIALS_HEADER);
            }
            try (CloseableHttpClient client = HttpClients.custom().setSSLContext(SslContext.sslClientCertValid).build()) {
                final HttpResponse response = client.execute(request);
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
            Set<String> expectedInstanceIds = registryCheckInstances.stream()
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
            for (var ds : get(CoreService.DISCOVERY)) {
                var documentContext = getDocumentAsContext(HttpRequestUtils.getUri(
                    ds.getScheme(), ds.getHostname(), ds.getPort(), "/eureka/apps"
                ), ds.getServiceConfiguration().isBasicAuthenticationSupported());
                if (documentContext == null || !areAllInstanceOnInEureka(documentContext)) {
                    return false;
                }
            }
            return true;
        }

        private int getEurekaVersion(Instance instance) {
            var documentContext = getDocumentAsContext(URI.create(instance.getEurekaVersionUrl()), instance.getServiceConfiguration().isBasicAuthenticationSupported());
            if (documentContext != null) {
                return documentContext.read("version");
            }
            log.debug("Eurekaversion endpoint is not accessible on {}", instance.getInstanceId());
            return -1;
        }

        private boolean areAllInstancesRegistryUpToDate() {
            List<String> notUpdated = new ArrayList<>();
            for (Instance instance : registryVersionCheckInstances) {
                int version = getEurekaVersion(instance);
                if (version < this.minimumEurekaVersion) {
                    notUpdated.add(String.format("%s (%d / %s)", instance.getInstanceId(), version, this.minimumEurekaVersion));
                }
            }
            if (notUpdated.isEmpty()) {
                return true;
            }

            log.debug("There are instances that has not been updated yet: {}", StringUtils.join(notUpdated, ", "));
            return false;
        }

        boolean areDiscoveryInSync() {
            int sharedVersion = -1;
            for (var discoveryServiceInstance : get(CoreService.DISCOVERY)) {
                int version = getEurekaVersion(discoveryServiceInstance);
                log.debug("Version at {} is {}", discoveryServiceInstance.getInstanceId(), version);
                if (version < 0) {
                    // eureka is not initialized yet
                    return false;
                }
                if (sharedVersion < 0) {
                    // first fetched value
                    sharedVersion = version;
                }
                if (sharedVersion != version) {
                    // versions are not in sync
                    return false;
                }
            }

            this.minimumEurekaVersion = sharedVersion;
            return true;
        }

        boolean areAllInstancesAreUp() {
            List<String> downInstances = new ArrayList<>();
            for (var instance : registryVersionCheckInstances) {
                var documentContext = getDocumentAsContext(URI.create(instance.getHealthEndpointUrl()), instance.getServiceConfiguration().isBasicAuthenticationSupported());
                String status = "N/A";
                if (documentContext != null) {
                    status = documentContext.read("status");
                }
                if (!"UP".equals(status)) {
                    downInstances.add(String.format("%s (%s)", instance.getInstanceId(), status));
                }
            }
            if (!downInstances.isEmpty()) {
                log.debug("Some instances are still down: {}", downInstances);
            }
            return downInstances.isEmpty();
        }

        boolean isAuthUp() {
            var serviceId = IS_MODULITH_ENABLED ? CoreService.GATEWAY : CoreService.ZAAS;
            var key = "$.components.zaas.details.auth";
            List<String> downZaasInstances = new ArrayList<>();
            for (var instance : get(serviceId)) {
                var documentContext = getDocumentAsContext(URI.create(instance.getHealthEndpointUrl()), instance.getServiceConfiguration().isBasicAuthenticationSupported());
                var status = "N/A";
                if (documentContext != null) {
                    try {
                        status = documentContext.read(key);
                    } catch (Exception e) {
                        log.debug("Cannot parse {} on {}", key, instance.getInstanceId());
                    }
                }
                if (!"UP".equals(status)) {
                    downZaasInstances.add(String.format("%s (%s)", instance.getInstanceId(), status));
                }
            }
            if (!downZaasInstances.isEmpty()) {
                log.debug("Authentication Service is not ready: {}", downZaasInstances);
            }
            return downZaasInstances.isEmpty();
        }

        public void waitUntilReady() {
            awaitFor(this::areAllInstancesOnboarded, 8);
            awaitFor(this::areDiscoveryInSync, 2);
            awaitFor(this::areAllInstancesRegistryUpToDate, 3);
            awaitFor(this::areAllInstancesAreUp, 2);
            awaitFor(this::isAuthUp, 1);
        }

        public List<Instance> get(CoreService type) {
            return allInstances.stream().filter(instance -> type.getServiceId().equals(instance.getServiceId())).toList();
        }

        public List<Instance> without(String... serviceIds) {
            return allInstances.stream().filter(i -> !Strings.CI.equalsAny(i.getServiceId(), serviceIds)).collect(Collectors.toList());
        }

        public List<Instance> without(CoreService... types) {
            var serviceIds = Arrays.stream(types).map(CoreService::getServiceId).toArray(String[]::new);
            return without(serviceIds);
        }

        public static List<ApimlInstance> load() {
            String centralHostsConfig = System.getProperty("centralHosts");
            if (StringUtils.isBlank(centralHostsConfig)) {
                return Collections.singletonList(new ApimlInstance(x -> true));
            }

            var centralHosts = Arrays.stream(centralHostsConfig.split("[,;]"))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
            var domain = new ApimlInstance(instance -> !centralHosts.contains(instance.getHostname().toLowerCase()));
            var central = new ApimlInstance(instance -> centralHosts.contains(instance.getHostname().toLowerCase()));
            central.allInstances.addAll(domain.get(CoreService.GATEWAY));
            central.registryCheckInstances.addAll(domain.get(CoreService.GATEWAY));

            log.debug("Domain = {}", domain.allInstances);
            log.debug("Central = {}", central.allInstances);

            return Arrays.asList(domain, central);
        }

    }

}
