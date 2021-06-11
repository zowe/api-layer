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

public class LoadBalancerConstants {
    public static String METADATA_KEY = "metadata";
    public static String INSTANCE_KEY = "instance";
    public static char CONFIG_SEPARATOR = '.';
    public static String SERVICEID_KEY = "serviceId";

    public static String getMetadataPrefix() {
        return INSTANCE_KEY + CONFIG_SEPARATOR + METADATA_KEY + CONFIG_SEPARATOR;
    }
}
