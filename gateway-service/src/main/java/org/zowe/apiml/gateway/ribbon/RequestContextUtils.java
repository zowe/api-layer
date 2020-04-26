/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;

import java.util.Optional;

/**
 * Class for storing and retrieving instance information from RequestContext
 */
public class RequestContextUtils {

    private RequestContextUtils() {}

    public static final String INSTANCE_INFO_KEY = "apimlLoadBalancedInstanceInfo";
    public static final String DEBUG_INFO_KEY = "apimlRibbonRetryDebug";

    public static Optional<InstanceInfo> getInstanceInfo() {
        Object o = RequestContext.getCurrentContext().get(INSTANCE_INFO_KEY);
        if (!(o instanceof InstanceInfo)) {
            return Optional.empty();
        } else {
            return Optional.of((InstanceInfo) o);
        }
    }

    public static void setInstanceInfo(InstanceInfo info) {
        RequestContext.getCurrentContext().set(INSTANCE_INFO_KEY, info);
    }

    public static void addDebugInfo(String debug) {
        String existingDebugInfo = getDebugInfo();
        RequestContext.getCurrentContext().set(DEBUG_INFO_KEY,
            existingDebugInfo.isEmpty() ? debug : existingDebugInfo + "|" + debug);
    }

    public static String getDebugInfo() {
        Object o = RequestContext.getCurrentContext().get(DEBUG_INFO_KEY);
        return o != null ? (String) o : "";
    }
}
