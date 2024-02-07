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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.gateway.filters.pre.LocationFilter;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.RoutedServicesUser;
import static org.mockito.Mockito.*;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RoutedServicesNotifierTest {

    @Mock
    LocationFilter user1;
    @Mock
    LocationFilter user2;

    private List<RoutedServicesUser> routedServicesUserList = new ArrayList<>();

    RoutedServicesNotifier underTest;

    @BeforeEach
    void setup() {
        routedServicesUserList.add(user1);
        routedServicesUserList.add(user2);
        underTest = new RoutedServicesNotifier(routedServicesUserList);
    }

    @Test
    void usersAreUpdatedOnlyAboutAddedServices() {

        underTest.addRoutedServices("service1", new RoutedServices());
        underTest.addRoutedServices("service2", new RoutedServices());

        underTest.notifyAndFlush();

        verify(user1, times(1)).addRoutedServices(eq("service1"),any());
        verify(user1, times(1)).addRoutedServices(eq("service2"),any());
        verify(user2, times(1)).addRoutedServices(eq("service1"),any());
        verify(user2, times(1)).addRoutedServices(eq("service2"),any());

        underTest.addRoutedServices("service1", new RoutedServices());
        underTest.notifyAndFlush();

        verify(user1, times(2)).addRoutedServices(eq("service1"),any());
        verify(user1, times(1)).addRoutedServices(eq("service2"),any());
        verify(user2, times(2)).addRoutedServices(eq("service1"),any());
        verify(user2, times(1)).addRoutedServices(eq("service2"),any());

    }
}
