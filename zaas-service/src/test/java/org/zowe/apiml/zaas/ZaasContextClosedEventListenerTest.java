/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ZaasContextClosedEventListenerTest {

    @Mock
    private ZaasStartupListener zaasStartupListener;

    private ZaasContextClosedEventListener zaasContextClosedEventListener;

    @BeforeEach
    void setUp() {
        zaasContextClosedEventListener = new ZaasContextClosedEventListener(zaasStartupListener);
    }

    @Test
    void testOnApplicationEvent() {
        doNothing().when(zaasStartupListener).onContextClosed();

        assertDoesNotThrow(() -> zaasContextClosedEventListener.onApplicationEvent(new ContextClosedEvent(mock(ApplicationContext.class)))
    );
    }

}
