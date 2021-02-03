/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FullApiMediationLayer {
    private RunningService discoveryService;
    private RunningService gatewayService;
    private RunningService apiCatalogService;
    private RunningService cachingService;
    private RunningService mockZosmfService;
    private RunningService discoverableClientService;

    private static FullApiMediationLayer instance = new FullApiMediationLayer();
    private FullApiMediationLayer() {
        prepareCaching();
        prepareCatalog();
        prepareDiscoverableClient();
        prepareDiscovery();
        prepareGateway();
        prepareMockZosmf();
    }

    private void prepareDiscovery() {
        Map<String, String> before = new HashMap<>();
        before.put("-Dloader.path", "build/libs/api-layer-lite-lib-all.jar");

        Map<String, String> after = new HashMap<>();
        after.put("--spring.profiles.active", "https");
        after.put("--spring.config.additional-location", "file:./config/local/discovery-service.yml");
        after.put("--apiml.security.ssl.verifySslCertificatesOfServices", "true");

        discoveryService = new RunningService("discovery", "discovery-service/build/libs/discovery-service-lite.jar", before, after);
    }

    private void prepareGateway() {
        Map<String, String> before = new HashMap<>();
        before.put("-Dloader.path", "build/libs/api-layer-lite-lib-all.jar");

        Map<String, String> after = new HashMap<>();
        after.put("--spring.config.additional-location", "file:./config/local/gateway-service.yml");
        after.put("--apiml.security.ssl.verifySslCertificatesOfServices", "true");

        gatewayService = new RunningService("gateway", "gateway-service/build/libs/gateway-service-lite.jar", before, after);

    }

    private void prepareCatalog() {
        Map<String, String> before = new HashMap<>();
        before.put("-Dloader.path", "build/libs/api-layer-lite-lib-all.jar");

        Map<String, String> after = new HashMap<>();
        after.put("--spring.config.additional-location", "file:./config/local/api-catalog-service.yml");
        after.put("--apiml.security.ssl.verifySslCertificatesOfServices", "true");

        apiCatalogService = new RunningService("apicatalog", "api-catalog-services/build/libs/api-catalog-services-lite.jar", before, after);
    }

    private void prepareCaching() {
        Map<String, String> before = new HashMap<>();
        Map<String, String> after = new HashMap<>();

        cachingService = new RunningService("cachingservice", "caching-service/build/libs/caching-service.jar", before, after);
    }

    private void prepareMockZosmf() {
        Map<String, String> before = new HashMap<>();
        Map<String, String> after = new HashMap<>();

        mockZosmfService = new RunningService("zosmf", "mock-zosmf/build/libs/mock-zosmf.jar", before, after);
    }

    private void prepareDiscoverableClient() {
        Map<String, String> before = new HashMap<>();
        Map<String, String> after = new HashMap<>();
        after.put("--spring.config.additional-location", "file:./config/local/discoverable-client.yml");

        discoverableClientService = new RunningService("discoverableclient", "discoverable-client/build/libs/discoverable-client.jar", before, after);
    }

    public static FullApiMediationLayer getInstance() {
        return instance;
    }

    public void start() {
        try {
            discoveryService.start();
            gatewayService.start();
            mockZosmfService.start();

            cachingService.start();
            apiCatalogService.start();
            discoverableClientService.start();
        } catch (IOException ex) {
            throw new RuntimeException("Couldn't start the services");
        }
    }

    public void stop() {
        discoveryService.stop();
        gatewayService.stop();
        mockZosmfService.stop();

        cachingService.stop();
        apiCatalogService.stop();
        discoverableClientService.stop();
    }
}
