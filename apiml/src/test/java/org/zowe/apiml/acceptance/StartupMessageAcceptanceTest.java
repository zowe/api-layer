/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.EurekaServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.zaas.ZaasServiceAvailableEvent;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@AcceptanceTest
@ExtendWith(MockitoExtension.class)
public class StartupMessageAcceptanceTest extends AcceptanceTestWithMockServices {

    @MockitoBean private ApimlLogger logger;

    @Mock private InstanceInfo instanceInfo;

    @BeforeEach
    void setUp() {
        lenient().when(instanceInfo.getInstanceId()).thenReturn("apicatalog:localhost:1000");
        lenient().when(instanceInfo.getAppName()).thenReturn("APICATALOG");

        applicationEventPublisher.publishEvent(new ZaasServiceAvailableEvent("dummy"));
        applicationEventPublisher.publishEvent(new EurekaRegistryAvailableEvent(mock(EurekaServerConfig.class)));
        applicationEventPublisher.publishEvent(new EurekaInstanceRegisteredEvent(new Object(), instanceInfo, DISCOVERY_PORT, false));
    }

    @Test
    @Timeout(unit = TimeUnit.SECONDS, value = 30)
    void whenFullyStartedUp_thenEmitMessage() {

        verify(logger, timeout(30_000)).log("org.zowe.apiml.common.mediationLayerStarted");

    }

}
