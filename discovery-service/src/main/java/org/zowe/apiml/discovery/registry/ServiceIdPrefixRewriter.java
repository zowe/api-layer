/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.regex.Pattern;

/**
 * Rewrites a service-id prefix on the way into the registry, driven by
 * {@code apiml.discovery.serviceIdPrefixReplacer}.
 * <p>
 * Used where one APIML fronts services whose ids collide, or carry a prefix that has to be presented differently.
 * The configuration value is a pair, {@code oldPrefix,newPrefix}; either may carry a trailing {@code *}.
 * <p>
 * Ported from {@code ApimlInstanceRegistry.changeServiceId} / {@code replaceValues}. The important thing the
 * original got right, and which is easy to lose: the rewrite has to apply to <em>renew, cancel and status
 * update</em> as well as to registration. A service that registered under the old prefix keeps heartbeating under
 * the old prefix, and those heartbeats have to find the rewritten entry or the instance is evicted while it is
 * still healthy. That is why this is applied by {@link PrefixRewritingServiceRegistry} around every write rather
 * than only by a registration interceptor.
 */
@Slf4j
public class ServiceIdPrefixRewriter {

    private final Tuple tuple;

    public ServiceIdPrefixRewriter(String configuredTuple) {
        this.tuple = new Tuple(configuredTuple);
    }

    public boolean enabled() {
        return tuple.isValid();
    }

    /** Rewrites an appName / instanceId pair. Returns the pair unchanged when disabled. */
    public String[] rewrite(String appName, String instanceId) {
        if (!tuple.isValid()) {
            return new String[]{appName, instanceId};
        }
        String appNameRegex = "(?i)^" + tuple.getOldPrefix();
        String instanceIdRegex = "(?i):" + tuple.getOldPrefix();
        String target = tuple.getNewPrefix().replace("*", "");

        String rewrittenAppName = appName == null ? null : appName.replaceAll(appNameRegex, target).toUpperCase();
        String rewrittenInstanceId = instanceId;
        if (instanceId != null) {
            rewrittenInstanceId = instanceId.contains(":")
                ? instanceId.replaceAll(instanceIdRegex, ":" + target)
                : instanceId.replaceAll(appNameRegex, target);
        }
        return new String[]{rewrittenAppName, rewrittenInstanceId};
    }

    /** Rewrites a whole instance, if its app name carries the old prefix. */
    public ServiceInstance rewrite(ServiceInstance instance) {
        if (!tuple.isValid()) {
            return instance;
        }
        String servicePrefix = tuple.getOldPrefix();
        if (!servicePrefix.contains("*")) {
            servicePrefix = servicePrefix + "*";
        }
        if (!Pattern.compile("(?i)^" + servicePrefix).matcher(instance.appName()).find()) {
            return instance;
        }

        String[] rewritten = rewrite(instance.appName(), instance.instanceId());
        log.debug("The instance ID of {} service has been changed to {}.", instance.appName(), rewritten[1]);
        return instance.toBuilder()
            .appName(rewritten[0])
            .appGroupName(rewritten[0])
            .instanceId(rewritten[1])
            .vipAddress(rewritten[0].toLowerCase())
            .build();
    }

    /** The configured {@code oldPrefix,newPrefix} pair. */
    @Getter
    public static class Tuple {

        private boolean valid;
        private String oldPrefix;
        private String newPrefix;

        public Tuple(String tuple) {
            if (isValidTuple(tuple)) {
                String[] prefixes = tuple.split(",");
                this.oldPrefix = prefixes[0];
                this.newPrefix = prefixes[1];
                this.valid = true;
            }
        }

        public static boolean isValidTuple(String tuple) {
            if (StringUtils.isNotEmpty(tuple)) {
                String[] replacer = tuple.split(",");
                return replacer.length > 1
                    && StringUtils.isNotEmpty(replacer[0])
                    && StringUtils.isNotEmpty(replacer[1])
                    && !replacer[0].equals(replacer[1]);
            }
            return false;
        }

    }

}
