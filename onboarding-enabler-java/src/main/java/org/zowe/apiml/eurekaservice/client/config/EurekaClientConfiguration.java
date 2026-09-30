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

import org.zowe.apiml.product.eureka.EurekaServiceUrlUtils;

import java.util.List;

/**
 * The enabler's client configuration, derived from {@link ApiMediationServiceConfig}.
 * <p>
 * This class used to extend Netflix's {@code DefaultEurekaClientConfig} and answered the Netflix client's questions
 * one getter at a time. The getters that carried real configuration across the migration are kept, under their
 * original names, so custom {@code EurekaClientConfigProvider} implementations keep working; the getters that only
 * made sense to Netflix's internals (decoder name, region, DNS-based service URL resolution) are gone with it.
 * <p>
 * The renewal and fetch cadence is what Netflix's {@code DefaultEurekaClientConfig} defaulted to, which the enabler
 * never overrode.
 */
public class EurekaClientConfiguration {

    private static final int DEFAULT_RENEWAL_INTERVAL = 30;
    private static final int DEFAULT_FETCH_INTERVAL = 30;

    private final ApiMediationServiceConfig config;

    public EurekaClientConfiguration(ApiMediationServiceConfig config) {
        this.config = config;
    }

    protected ApiMediationServiceConfig getConfig() {
        return config;
    }

    /**
     * The Discovery Service URLs to talk to, with basic authentication credentials embedded as
     * {@code scheme://userid:password@host:port/path} when they are configured - the representation the registry
     * client's transport understands and extracts itself.
     */
    public List<String> getEurekaServerServiceUrls() {
        String password = (config.getDiscoveryPassword() == null) ? null : new String(config.getDiscoveryPassword());
        return EurekaServiceUrlUtils.addCredentials(config.getDiscoveryServiceUrls(), config.getDiscoveryUserid(), password);
    }

    /**
     * How often the cached view of the registry is brought up to date, in seconds.
     */
    public int getRegistryFetchIntervalSeconds() {
        return DEFAULT_FETCH_INTERVAL;
    }

    /**
     * How often the lease is renewed, in seconds. This is also the interval the Discovery Service is told to expect
     * renewals at through the instance's lease, so the two must not drift apart.
     */
    public int getLeaseRenewalIntervalInSeconds() {
        return DEFAULT_RENEWAL_INTERVAL;
    }

    public int getEurekaServerConnectTimeoutSeconds() {
        return config.getConnectTimeout();
    }

    public int getEurekaServerReadTimeoutSeconds() {
        return config.getReadTimeout();
    }

}
