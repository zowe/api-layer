/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import com.netflix.appinfo.InstanceInfo;
import lombok.experimental.UtilityClass;

/**
 * Base URL of a Netflix {@code InstanceInfo}.
 * <p>
 * Extracted from {@code EurekaUtils} so that common-service-core - which every module depends on, including the
 * Discovery Service, which no longer uses Eureka at all - does not have to carry eureka-client and its tail
 * (archaius, xstream, servo, jettison). Every other method on {@code EurekaUtils} is Netflix-free and stayed put.
 * <p>
 * Deleted with the rest of this module once ZAAS moves onto the replacement client.
 */
@UtilityClass
public class EurekaInstanceUrls {

    /**
     * @return {@code https://host:securePort} when the secure port is enabled, otherwise {@code http://host:port}
     */
    public String getUrl(InstanceInfo instanceInfo) {
        if (instanceInfo.getSecurePort() == 0 || !instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE)) {
            return "http://" + instanceInfo.getHostName() + ":" + instanceInfo.getPort();
        } else {
            return "https://" + instanceInfo.getHostName() + ":" + instanceInfo.getSecurePort();
        }
    }

}
