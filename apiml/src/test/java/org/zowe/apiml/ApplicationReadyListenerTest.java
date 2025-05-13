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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.zowe.apiml.discovery.staticdef.StaticServicesRegistrationService;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ApplicationReadyListenerTest {

    @Mock private StaticServicesRegistrationService registrationService;

    @InjectMocks
    private ApplicationReadyListener listener;

    @Test
    void onEvent_registerServices() {
        doNothing().when(registrationService).registerServices();

        listener.onApplicationEvent(mock(ApplicationReadyEvent.class));
        verify(registrationService).registerServices();
    }

}
