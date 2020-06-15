/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.metadata.service;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class EurekaApplicationsTest {

    @Mock
    private EurekaClient eurekaClient;

    @Test
    void givenEurekaClient_whenRetrieveRegisteredApps_thenReturnApps() {
        EurekaApplications retrieval = new EurekaApplications(eurekaClient);
        Applications applications = mock(Applications.class);
        List<Application> applicationList = new ArrayList<>();
        applicationList.add(mock(Application.class));

        Mockito.when(eurekaClient.getApplications()).thenReturn(applications);
        Mockito.when(eurekaClient.getApplications().getRegisteredApplications()).thenReturn(applicationList);

        List<Application> registeredApps = retrieval.getRegistered();

        assertFalse(registeredApps.isEmpty());
    }
}
