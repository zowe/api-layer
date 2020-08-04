/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.routing;

import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.RoutedServicesUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutedServicesNotifier {

    private final List<RoutedServicesUser> routedServicesUserList;
    private Map<String, RoutedServices> buffer = new HashMap<>();

    public RoutedServicesNotifier(List<RoutedServicesUser> routedServicesUserList) {
        this.routedServicesUserList = routedServicesUserList;
    }

    public void notifyAndFlush() {
        routedServicesUserList.forEach(routedServicesUser ->
            buffer.forEach(routedServicesUser::addRoutedServices)
        );
        flushBuffer();
    }

    private void flushBuffer() {
        buffer = new HashMap<>();
    }

    public void addRoutedServices(String service, RoutedServices routedServices) {
        buffer.put(service, routedServices);
    }
}
