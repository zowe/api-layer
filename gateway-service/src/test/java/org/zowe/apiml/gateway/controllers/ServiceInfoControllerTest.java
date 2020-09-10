/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.controllers;

import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.metadata.service.EurekaApplications;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceInfoControllerTest {

    private ServiceInfoController serviceInfoController;
    private EurekaApplications applications;

    @BeforeEach
    void setup() {
        applications = mock(EurekaApplications.class);
        serviceInfoController = new ServiceInfoController(applications);
    }

    @Test
    void givenExistingServiceId_returnTrue() {
        when(applications.getRegistered()).thenReturn(Collections.singletonList(new Application("test-service")));
        assertTrue(serviceInfoController.isServiceRegistered("test-service"));
    }

    @Test
    void givenNonExistingServiceId_returnFalse() {
        when(applications.getRegistered()).thenReturn(Collections.singletonList(new Application("test-service")));
        assertFalse(serviceInfoController.isServiceRegistered("non-existing-service"));
    }

    @Test
    void givenServiceIdButNoServicesAreRegistered_returnFalse() {
        when(applications.getRegistered()).thenReturn(Collections.emptyList());
        assertFalse(serviceInfoController.isServiceRegistered("non-existing-service"));
    }
}
