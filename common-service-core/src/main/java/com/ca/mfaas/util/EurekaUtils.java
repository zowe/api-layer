/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.util;

import com.netflix.appinfo.InstanceInfo;

/**
 * This util offer basic operation with eureka, like: extraction serviceId from instanceId, construct URL by
 * InstanceInfo etc.
 */
public final class EurekaUtils {

    private EurekaUtils() {

    }

    /**
     * Extract serviceId from instanceId
     * @param instanceId input, instanceId in format "host:service:random number to unique instanceId"
     * @return second part, it means serviceId. If it doesn't exist return null;
     */
    public static final String getServiceIdFromInstanceId(String instanceId) {
        final int startIndex = instanceId.indexOf(':');
        if (startIndex < 0) return null;

        final int endIndex = instanceId.indexOf(':', startIndex + 1);
        if (endIndex < 0) return null;

        return instanceId.substring(startIndex + 1, endIndex);
    }

    /**
     * Construct base URL for specific InstanceInfo
     * @param instanceInfo Instance of service, for which we want to get an URL
     * @return URL to the instance
     */
    public static final String getUrl(InstanceInfo instanceInfo) {
        if (instanceInfo.getSecurePort() == 0) {
            return "http://" + instanceInfo.getIPAddr() + ":" + instanceInfo.getPort();
        } else {
            return "https://" + instanceInfo.getIPAddr() + ":" + instanceInfo.getSecurePort();
        }
    }

}
