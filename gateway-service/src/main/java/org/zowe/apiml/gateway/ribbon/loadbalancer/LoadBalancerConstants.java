/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer;

/**
 * Static holder for load balancer constants
 */
public class LoadBalancerConstants {

    public static final String METADATA_KEY = "metadata";
    public static final String INSTANCE_KEY = "instance";
    public static final char CONFIG_SEPARATOR = '.';
    public static final String SERVICEID_KEY = "serviceId";
    public static final String INSTANCE_HEADER_KEY = "X-InstanceId";

    private LoadBalancerConstants() {}

    public static String getMetadataPrefix() {
        return INSTANCE_KEY + CONFIG_SEPARATOR + METADATA_KEY + CONFIG_SEPARATOR;
    }
}
