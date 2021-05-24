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

import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.startup.impl.ApiMediationLayerStartupChecker;
import org.zowe.apiml.util.config.ConfigReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO this class doesn't lend itself well to switching of configurations.
//attls is integrated in a kludgy way, and deserves a rewrite

@Slf4j
public class FullApiMediationLayer {
    private RunningService discoveryService;
    private RunningService gatewayService;
    private RunningService apiCatalogService;
    private RunningService cachingService;
    private RunningService mockZosmfService;
    private RunningService discoverableClientService;

    private ProcessBuilder nodeJsBuilder;
    private Process nodeJsSampleApp;

    private boolean firstCheck = true;
    private final Map<String, String> env;
    private static final boolean attlsEnabled = "true".equals(System.getProperty("environment.attls"));

    private static final FullApiMediationLayer instance = new FullApiMediationLayer();


    private FullApiMediationLayer() {
        env = ConfigReader.environmentConfiguration().getInstanceEnv();

        prepareCaching();
        prepareCatalog();
        prepareDiscoverableClient();
        prepareGateway();
        prepareMockZosmf();
        prepareDiscovery();
        if (!attlsEnabled) {
            prepareNodeJsSampleApp();
        }
    }

    private void prepareNodeJsSampleApp() {
        List<String> parameters = new ArrayList<>();
        parameters.add("node");
        parameters.add("src/index.js");

        ProcessBuilder builder1 = new ProcessBuilder(parameters);
        builder1.directory(new File("../onboarding-enabler-nodejs-sample-app/"));
        nodeJsBuilder = builder1.inheritIO();
    }

    private void prepareDiscovery() {
        discoveryService = new RunningService("discovery", "discovery-service/build/libs", null, null);
    }

    private void prepareGateway() {
        gatewayService = new RunningService("gateway", "gateway-service/build/libs", null, null);
    }

    private void prepareCatalog() {
        apiCatalogService = new RunningService("apicatalog", "api-catalog-services/build/libs", null, null);
    }

    public void prepareCaching() {
        cachingService = new RunningService("cachingservice", "caching-service/build/libs", null, null);
    }

    private void prepareMockZosmf() {
        Map<String, String> before = new HashMap<>();
        Map<String, String> after = new HashMap<>();
        if (attlsEnabled) {
            before.put("-Dspring.profiles.active", "attls");
        }
        mockZosmfService = new RunningService("zosmf", "mock-zosmf/build/libs/mock-zosmf.jar", before, after);
    }

    private void prepareDiscoverableClient() {
        Map<String, String> before = new HashMap<>();
        Map<String, String> after = new HashMap<>();
        if (attlsEnabled) {
            before.put("-Dspring.profiles.active", "attls");
        }

        after.put("--spring.config.additional-location", "file:./config/local/discoverable-client.yml");

        discoverableClientService = new RunningService("discoverableclient", "discoverable-client/build/libs/discoverable-client.jar", before, after);
    }

    public static FullApiMediationLayer getInstance() {
        return instance;
    }

    public void start() {
        try {
            log.info("Start Discovery Service");

            discoveryService.startWithScript("discovery-package/src/main/resources/bin/start.sh", env);

            log.info("Start Gateway Service");
            gatewayService.startWithScript("gateway-package/src/main/resources/bin/start.sh", env);

            log.info("Start Mock Zosmf Service");
            mockZosmfService.start();

            log.info("Start API Catalog Service");
            apiCatalogService.startWithScript("api-catalog-package/src/main/resources/bin/start.sh", env);

            log.info("Start Discoverable client");
            discoverableClientService.start();

            if (!attlsEnabled) {
                log.info("Node Js Sample App");
                nodeJsSampleApp = nodeJsBuilder.start();
            }

            log.info("Caching service starting");
            cachingService.startWithScript("caching-service-package/src/main/resources/bin/start.sh", env);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void stop() {
        try {
            discoveryService.stop();
            gatewayService.stop();
            mockZosmfService.stop();

            apiCatalogService.stop();
            discoverableClientService.stop();

            cachingService.stop();
            if (!attlsEnabled && !runsOffPlatform()) {
                nodeJsSampleApp.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean runsOffPlatform() {
        String offPlatform = System.getProperty("environment.offPlatform");
        return offPlatform != null && !offPlatform.isEmpty() && Boolean.parseBoolean(offPlatform);
    }

    public void waitUntilReady() {
        if (firstCheck) {
            new ApiMediationLayerStartupChecker().waitUntilReady();

            firstCheck = false;
        }
    }
}
