/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util.requests.ha;

import org.zowe.apiml.util.requests.GatewayRequests;
import org.zowe.apiml.util.requests.JsonResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zowe.apiml.util.config.ConfigReader.environmentConfiguration;

public class HAGatewayRequests {
    public List<GatewayRequests> gatewayServices = new ArrayList<>();

    public HAGatewayRequests() {
        String[] discoveryHosts = environmentConfiguration().getDiscoveryServiceConfiguration().getHost().split(",");
        for (String host : discoveryHosts) {
            gatewayServices.add(new GatewayRequests(host));
        }
    }

    public int existing() {
        return gatewayServices.size();
    }

    public boolean up(int instance) {
        return gatewayServices.get(instance).isUp();
    }

    public boolean up() {
        AtomicBoolean allUp = new AtomicBoolean(true);

        gatewayServices.parallelStream().forEach(service -> {
            if (!service.isUp()) {
                allUp.set(false);
            }
        });

        return allUp.get();
    }

    public void shutdown(int instance) {
        gatewayServices.get(instance).shutdown();
    }

    public void shutdown() {
        gatewayServices.parallelStream()
            .forEach(GatewayRequests::shutdown);
    }

    public JsonResponse route(int instance, String path) {
        return gatewayServices.get(instance).route(path);
    }

    public List<JsonResponse> route(String path) {
        List<JsonResponse> responses = new ArrayList<>();

        gatewayServices.parallelStream().forEach(service -> {
            responses.add(service.route(path));
        });

        return responses;
    }
}
