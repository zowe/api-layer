/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatMonitor;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.discovery.event.ParentHeartbeatEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RouteRefreshListenerTest {

    @Mock private ApplicationEventPublisher publisher;
    @Mock private HeartbeatMonitor heartbeatMonitor;

    private RouteRefreshListener routeRefreshListener;

    @BeforeEach
    void setUp() {
        routeRefreshListener = new RouteRefreshListener(publisher);
        ReflectionTestUtils.setField(routeRefreshListener, "monitor", heartbeatMonitor);
        lenient().when(heartbeatMonitor.update(any())).thenReturn(false);
    }

    @Nested
    class OnApplicationEvent {

        @Test
        void whenOtherEvent_thenIgnore() {
            routeRefreshListener.onApplicationEvent(new LogoutSuccessEvent(mock(Authentication.class)));
            verifyNoInteractions(heartbeatMonitor);
            verifyNoInteractions(publisher);
        }

        @Test
        void whenContextRefreshed_thenReset() {
            var ctx = mock(ApplicationContext.class);
            doNothing().when(publisher).publishEvent(any());
            routeRefreshListener.onApplicationEvent(new ContextRefreshedEvent(ctx));
            verify(publisher).publishEvent(any());
        }

        @Test
        void whenHeartbeatAndStatusChange_thenReset() {
            when(heartbeatMonitor.update(any())).thenReturn(true);
            doNothing().when(publisher).publishEvent(any());
            routeRefreshListener.onApplicationEvent(new HeartbeatEvent(mock(Object.class), mock(Object.class)));
            verify(publisher).publishEvent(any());
        }

        @Test
        void whenHeartbeatAndNoStatusChange_thenNothing() {
            routeRefreshListener.onApplicationEvent(new HeartbeatEvent(mock(Object.class), mock(Object.class)));
            verify(heartbeatMonitor, times(1)).update(any());
            verifyNoInteractions(publisher);
        }

        @Test
        void whenRefreshScope_thenReset() {
            doNothing().when(publisher).publishEvent(any());
            routeRefreshListener.onApplicationEvent(new RefreshScopeRefreshedEvent());
            verifyNoInteractions(heartbeatMonitor);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Test
        void whenInstanceRegistered_thenReset() {
            doNothing().when(publisher).publishEvent(any());
            routeRefreshListener.onApplicationEvent(new InstanceRegisteredEvent(mock(Object.class), mock(Object.class)));
            verifyNoInteractions(heartbeatMonitor);
        }

        @Test
        void whenEurekaInstanceRegistered_thenReset() {
            doNothing().when(publisher).publishEvent(any());
            routeRefreshListener.onApplicationEvent(new EurekaInstanceRegisteredEvent(mock(Object.class), mock(InstanceInfo.class), 0, false));
            verifyNoInteractions(heartbeatMonitor);
        }

        @Test
        void whenParentHeartbeatAndStatusChange_thenReset() {
            when(heartbeatMonitor.update(any())).thenReturn(true);
            doNothing().when(publisher).publishEvent(any());
            routeRefreshListener.onApplicationEvent(new ParentHeartbeatEvent(mock(Object.class), mock(Object.class)));
            verify(publisher).publishEvent(any());
        }

        @Test
        void whenParentheartbeatAndNoStatusChange_thenNothing() {
            routeRefreshListener.onApplicationEvent(new ParentHeartbeatEvent(mock(Object.class), mock(Object.class)));
            verify(heartbeatMonitor, times(1)).update(any());
            verifyNoInteractions(publisher);
        }

    }

}
