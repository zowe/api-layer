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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import org.zowe.apiml.filter.AttlsHttpHandler;
import org.zowe.apiml.product.web.ApimlTomcatCustomizer;
import org.zowe.apiml.zaas.ZaasServiceAvailableEvent;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StartupMessageAcceptanceTest {

    @AcceptanceTest
    @ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
    abstract static class BaseStartupTest extends AcceptanceTestWithMockServices {
        @Mock
        private InstanceInfo instanceInfo;

        @BeforeEach
        void setUp() {
            lenient().when(instanceInfo.getInstanceId()).thenReturn("apicatalog:localhost:1000");
            lenient().when(instanceInfo.getAppName()).thenReturn("APICATALOG");

            applicationEventPublisher.publishEvent(new ZaasServiceAvailableEvent("dummy"));
            applicationEventPublisher.publishEvent(new EurekaRegistryAvailableEvent(mock(EurekaServerConfig.class)));
            applicationEventPublisher.publishEvent(new EurekaInstanceRegisteredEvent(new Object(), instanceInfo, DISCOVERY_PORT, false));
        }

        void verifyStartupMessage(CapturedOutput output) {
            await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    String logOutput = output.getAll();
                    return logOutput.contains("ZWEAM001I");
                });

            String logOutput = output.getAll();
            assertTrue(logOutput.contains("API Mediation Layer started"));
        }
    }

    @Nested
    class GivenDefaultProfile extends BaseStartupTest {
        @Test
        void whenFullyStartedUp_thenEmitMessage(CapturedOutput output) {
            verifyStartupMessage(output);
        }
    }

    @Nested
    @ActiveProfiles({"attlsClient", "attlsServer"})
    class GivenAttlsProfile extends BaseStartupTest {
        @MockitoBean
        private AttlsHttpHandler attlsHttpHandler;
        @MockitoBean
        private ApimlTomcatCustomizer apimlTomcatCustomizer;
        @MockitoBean
        private ApimlInstanceRegistry apimlInstanceRegistry;

        @Test
        void whenFullyStartedUp_thenEmitMessage(CapturedOutput output) {
            // Prevent use of native code but verify it calls the customizer
            when(apimlInstanceRegistry.getApplications()).thenReturn(applicationRegistry.getApplications());
            doNothing().when(apimlTomcatCustomizer).customize(any());
            verifyStartupMessage(output);
        }
    }
}
