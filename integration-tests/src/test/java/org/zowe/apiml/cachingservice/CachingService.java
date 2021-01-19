/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.cachingservice;

import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.util.DiscoveryRequests;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static io.restassured.RestAssured.given;

@Slf4j
public class CachingService {
    private final String id = "cachingoldest";
    private final String evictionStrategy = "removeOldest";
    private final String port = "10022";

    private DiscoveryRequests discovery = new DiscoveryRequests();
    private Process newCachingProcess;

    public void start() throws IOException {
        log.info("Starting new Caching Service with ID {} on the port {}", id, port);
        stop();

        isServiceProperlyRegistered();

        ArrayList<String> discoveryCommand = new ArrayList<>();
        discoveryCommand.add("java");
        discoveryCommand.add("-Dcaching.storage.evictionStrategy=" + evictionStrategy);
        discoveryCommand.add("-Dapiml.service.serviceId=" + id);
        discoveryCommand.add("-Dapiml.service.port=" + port);
        discoveryCommand.add("-jar");
        discoveryCommand.add("../caching-service/build/libs/caching-service.jar");

        ProcessBuilder builder1 = new ProcessBuilder(discoveryCommand);
        newCachingProcess = builder1.inheritIO().start();
    }

    public boolean isServiceProperlyRegistered() {
        try {
            discovery.getApplications();
        } catch (URISyntaxException e) {
            // Ignorable exception.
            e.printStackTrace();
        }

        return false; //given().accept(ContentType.JSON).when().get("http://localhost:8761/eureka/apps").body().asString().contains("hazelcast-node:9090");
    }

    // Verify that the service is up and registered before actually using the service.

    public URI getBaseUrl() {
        return HttpRequestUtils.getUriFromGateway("/" + id + "/api/v1/cache");
    }

    public void stop() {
        if (newCachingProcess != null) {
            log.info("Stopping new Caching Service with ID {} on the port {}", id, port);
            newCachingProcess.destroy();
        }
    }
}
