/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client.config;

import com.netflix.discovery.DefaultEurekaClientConfig;
import org.zowe.apiml.product.eureka.EurekaServiceUrlUtils;

import java.util.List;

public class EurekaClientConfiguration extends DefaultEurekaClientConfig {
    private static final int DEFAULT_RENEWAL_INTERVAL = 30;
    private final ApiMediationServiceConfig config;

    public EurekaClientConfiguration(ApiMediationServiceConfig config) {
        this.config = config;
    }

    protected ApiMediationServiceConfig getConfig() {
        return config;
    }

    @Override
    public boolean shouldRegisterWithEureka() {
        return true;
    }

    @Override
    public String getDecoderName() {
        return "JacksonJson";
    }

    @Override
    public String getRegion() {
        return "default";
    }

    @Override
    public boolean shouldUseDnsForFetchingServiceUrls() {
        return false;
    }

    @Override
    public List<String> getEurekaServerServiceUrls(String s) {
        List<String> discoveryServiceUrls = config.getDiscoveryServiceUrls();
        // Embed credentials into the discovery URL when they are configured. The Netflix Eureka client
        // performs basic authentication only when credentials are present in the service URL. Credentials
        // are set when TLS validation of services is disabled on the Discovery Service side, in which case
        // the client certificate cannot be trusted and basic authentication is required instead.
        // EurekaServiceUrlUtils.addCredentials is a no-op when either credential is blank.
        String password = (config.getEurekaPassword() == null) ? null : new String(config.getEurekaPassword());
        return EurekaServiceUrlUtils.addCredentials(discoveryServiceUrls, config.getEurekaUserid(), password);
    }

    @Override
    public boolean shouldOnDemandUpdateStatusChange() {
        return false;
    }

    @Override
    public int getRegistryFetchIntervalSeconds() {
        return DEFAULT_RENEWAL_INTERVAL;
    }

    @Override
    public int getEurekaServerConnectTimeoutSeconds() {
        return config.getConnectTimeout();
    }

    @Override
    public int getEurekaServerReadTimeoutSeconds() {
        return config.getReadTimeout();
    }

}
