/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How this service describes itself when it registers.
 * <p>
 * Bound from {@code eureka.instance.*}. The prefix is kept on purpose. These properties are not an internal
 * detail - they are set by Zowe's {@code zowe.yaml}, by each service's {@code application.yml}, by z/OS start
 * procedures and by site overrides, and every onboarded extender's configuration too. Renaming them would be a
 * migration for every existing installation, which is a separate decision from replacing the implementation
 * behind them. Phase 7 can introduce an {@code apiml.registry.instance.*} alias with the old prefix deprecated.
 * <p>
 * The field set and the defaults mirror Netflix's {@code EurekaInstanceConfigBean}, including its quirks:
 * {@code metadataMap} is declared {@code Map<String, String>} at the same nesting depth so Spring's relaxed
 * binding flattens nested YAML into exactly the same keys it produced before, whatever those keys are.
 * {@code RegistryInstancePropertiesContractTest} binds real service YAML into both classes and fails on any
 * difference, so this is checked rather than asserted.
 * <p>
 * Two Netflix properties are deliberately absent, because {@code EurekaInstanceConfigBean} has no setter for
 * them and so they never bound in the first place: {@code eureka.instance.port} (only
 * {@code non-secure-port} exists) and {@code eureka.instance.dataCenterInfo}. The API Catalog's
 * {@code application.yml} sets {@code eureka.instance.port} to this day; it has always been ignored, and
 * honouring it now would change which port the Catalog advertises.
 */
@Data
@ConfigurationProperties("eureka.instance")
public class RegistryInstanceProperties {

    static final String UNKNOWN = "unknown";

    /** Service id. Defaulted from {@code spring.application.name} by the autoconfiguration. */
    private String appname = UNKNOWN;

    private String appGroupName;

    /** Unique among the instances of this service. Defaulted to {@code host:appname:port}. */
    private String instanceId;

    private String hostname;

    private String ipAddress;

    /** When set, {@link #resolvedHostname()} advertises the IP rather than the host name. */
    private boolean preferIpAddress = false;

    private int nonSecurePort = 80;

    private int securePort = 443;

    private boolean nonSecurePortEnabled = true;

    private boolean securePortEnabled = false;

    private int leaseRenewalIntervalInSeconds = 30;

    private int leaseExpirationDurationInSeconds = 90;

    private String virtualHostName = UNKNOWN;

    private String secureVirtualHostName = UNKNOWN;

    private String homePageUrlPath = "/";

    private String homePageUrl;

    private String statusPageUrlPath;

    private String statusPageUrl;

    private String healthCheckUrlPath;

    private String healthCheckUrl;

    private String secureHealthCheckUrl;

    /**
     * When false the instance registers as {@code STARTING} and only becomes {@code UP} once it reports
     * healthy, so nothing routes to it while it is still coming up.
     */
    private boolean instanceEnabledOnit = false;

    private Map<String, String> metadataMap = new LinkedHashMap<>();

    // -------------------------------------------------------------------------------------------------------
    // Derivations. These reproduce what Netflix's InstanceInfo.Builder did with the same inputs.
    // -------------------------------------------------------------------------------------------------------

    public String resolvedHostname() {
        return preferIpAddress && ipAddress != null ? ipAddress : hostname;
    }

    /** The port an unsecured URL would use: the secure port when TLS is on, the plain port otherwise. */
    public int advertisedPort() {
        return securePortEnabled ? securePort : nonSecurePort;
    }

    public String resolvedHomePageUrl() {
        return absoluteUrl(homePageUrl, homePageUrlPath);
    }

    public String resolvedStatusPageUrl() {
        return absoluteUrl(statusPageUrl, statusPageUrlPath);
    }

    /**
     * Unlike the home page and status page, the plain health-check URL is only derived from a relative path
     * when the non-secure port is enabled - verified against {@code InstanceInfo.Builder.setHealthCheckUrls}
     * in the eureka-client bytecode, where both health-check fallbacks are port-guarded and the other two URL
     * setters are not. The API Catalog's own configuration depends on this: it disables the non-secure port
     * and sets {@code healthCheckUrlPath}, and registers no plain health-check URL at all.
     */
    public String resolvedHealthCheckUrl() {
        if (healthCheckUrl != null) {
            return healthCheckUrl;
        }
        if (healthCheckUrlPath == null || !nonSecurePortEnabled) {
            return null;
        }
        return "http://" + resolvedHostname() + ":" + nonSecurePort + healthCheckUrlPath;
    }

    /** Mirrors {@link #resolvedHealthCheckUrl()}, guarded by the secure port instead. */
    public String resolvedSecureHealthCheckUrl() {
        if (secureHealthCheckUrl != null) {
            return secureHealthCheckUrl;
        }
        if (healthCheckUrlPath == null || !securePortEnabled) {
            return null;
        }
        return "https://" + resolvedHostname() + ":" + securePort + healthCheckUrlPath;
    }

    /**
     * Netflix built relative-path fallbacks as {@code http://host:port + path} - plain http, and the
     * non-secure port, even on a TLS-only instance. APIML's own services all set the explicit URLs, so this
     * only affects an onboarded service that supplies a path alone; reproduced as it was rather than
     * corrected, because a service registering a suddenly-different status page URL is a behaviour change
     * that belongs in its own commit.
     */
    private String absoluteUrl(String explicit, String path) {
        if (explicit != null) {
            return explicit;
        }
        if (path == null) {
            return null;
        }
        return "http://" + resolvedHostname() + ":" + nonSecurePort + path;
    }

}
