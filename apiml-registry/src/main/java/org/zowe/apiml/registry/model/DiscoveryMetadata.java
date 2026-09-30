/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.model;

/**
 * Reserved metadata keys used to carry registry fields that Spring Cloud's {@code ServiceInstance} has no room
 * for.
 * <p>
 * Spring's interface models a host, a port, a secure flag and a metadata map - nothing else. The registry knows
 * more than that, and a couple of consumers legitimately need one of the extras: the API Catalog links to a
 * service's home page, which is a full URL with a path, not something that can be reconstructed from
 * {@code getUri()}. Rather than have those consumers downcast to a registry-specific type - which is exactly the
 * {@code EurekaServiceInstance} cast this work is removing - the adapters put the value here.
 * <p>
 * Deliberately limited to display data. Anything used for a security decision is not passed this way: it either
 * belongs on the Spring interface already or is checked where the full registry model is available.
 */
public final class DiscoveryMetadata {

    /** The instance's advertised home page, path included. */
    public static final String HOME_PAGE_URL = "apiml.registry.homePageUrl";

    /**
     * The instance's effective status, override applied.
     * <p>
     * Carried so the API Catalog can keep reporting a container as DOWN. In practice a {@code DiscoveryClient}
     * only offers instances that are up - Eureka's client filtered to UP by default too, so that display path was
     * already unreachable in production - but the capability is preserved rather than quietly deleted, and it
     * works if a discovery client is ever configured to return more.
     */
    public static final String INSTANCE_STATUS = "apiml.registry.status";

    private DiscoveryMetadata() {
    }

}
