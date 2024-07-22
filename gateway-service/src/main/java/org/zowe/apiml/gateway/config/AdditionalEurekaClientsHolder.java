/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;

import java.util.List;

/**
 * Purpose of this holder is to keep additional {@link CloudEurekaClient} instances in the custom bean so that it does not interfere with standard `primary` eureka autoconfiguration.
 * <p>
 * Wrapper exposes proxy `shutdown` call of the client instances
 */
@Getter
@RequiredArgsConstructor
public class AdditionalEurekaClientsHolder {
    private final List<CloudEurekaClient> discoveryClients;

    public void shutdown() {
        if (discoveryClients != null) {
            discoveryClients.forEach(CloudEurekaClient::shutdown);
        }
    }
}
