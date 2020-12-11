/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.services.cached;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

class CachedServicesServiceTest {

    @Test
    void testReturningNoServices() {
        CachedServicesService cachedServicesService = new CachedServicesService();
        cachedServicesService.clearAllServices();
        Applications services = cachedServicesService.getAllCachedServices();
        Assertions.assertNull(services);
    }

    @Test
    void testReturningOneService() {
        CachedServicesService cachedServicesService = new CachedServicesService();
        cachedServicesService.clearAllServices();
        Applications applications = cachedServicesService.getAllCachedServices();
        Assertions.assertNull(applications);

        InstanceInfo instance = getStandardInstance("service", InstanceInfo.InstanceStatus.DOWN, null);
        Application application = new Application("service");
        application.addInstance(instance);
        cachedServicesService.updateService("service", application);

        applications = cachedServicesService.getAllCachedServices();
        Assertions.assertNotNull(applications.getRegisteredApplications());
    }

    @Test
    void testGetAService() {
        CachedServicesService cachedServicesService = new CachedServicesService();
        cachedServicesService.clearAllServices();
        Applications applications = cachedServicesService.getAllCachedServices();
        Assertions.assertNull(applications);

        InstanceInfo instance = getStandardInstance("service", InstanceInfo.InstanceStatus.DOWN, null);
        Application application = new Application("service");
        application.addInstance(instance);
        cachedServicesService.updateService("service", application);

        Application service = cachedServicesService.getService("service");
        Assertions.assertNotNull(service);
        Assertions.assertEquals("service", service.getName());
    }

    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status,
            HashMap<String, String> metadata) {
        return new InstanceInfo(serviceId, serviceId.toUpperCase(), null, "192.168.0.1", null,
                new InstanceInfo.PortWrapper(true, 9090), null, null, null, null, null, null, null, 0, null, "hostname",
                status, null, null, null, null, metadata, null, null, null, null);
    }
}
