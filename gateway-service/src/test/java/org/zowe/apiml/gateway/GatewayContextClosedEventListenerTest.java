/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GatewayContextClosedEventListenerTest {

    @Mock
    private GatewayStartupListener gatewayStartupListener;

    private GatewayContextClosedEventListener gatewayContextClosedEventListener;

    @BeforeEach
    void setUp() {
        gatewayContextClosedEventListener = new GatewayContextClosedEventListener(gatewayStartupListener);
    }

    @Test
    void testOnApplicationEvent() {
        doNothing().when(gatewayStartupListener).onContextClosed();

        gatewayContextClosedEventListener.onApplicationEvent(new ContextClosedEvent(mock(ApplicationContext.class)));

        verify(gatewayStartupListener).onContextClosed();
    }

}
