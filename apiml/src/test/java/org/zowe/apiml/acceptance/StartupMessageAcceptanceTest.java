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
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import org.zowe.apiml.product.web.ApimlTomcatCustomizer;
import org.zowe.apiml.zaas.ZaasServiceAvailableEvent;
import org.zowe.commons.attls.AttlsContext;
import org.zowe.commons.attls.InboundAttls;
import org.zowe.commons.attls.IoctlCallException;
import org.zowe.commons.attls.StatConn;
import org.zowe.commons.attls.UnknownEnumValueException;

import java.util.Base64;
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

        private static final String CERTIFICATE = "MIID3jCCAsagAwIBAgIULApMeb1+40+ifLXNVf1mqwsNlt4wDQYJKoZIhvcNAQEL" +
            "BQAwYDELMAkGA1UEBhMCQ1oxEDAOBgNVBAgMB0N6ZWNoaWExDzANBgNVBAcMBlBy" +
            "YWd1ZTEMMAoGA1UECgwDT01GMQ0wCwYDVQQLDARab3dlMREwDwYDVQQDDAhBUElN" +
            "TCBDQTAeFw0yMzA2MDIxMjQ4NDBaFw0yOTA1MzExMjQ4NDBaMF8xCzAJBgNVBAYT" +
            "AkNaMRAwDgYDVQQIDAdDemVjaGlhMQ8wDQYDVQQHDAZQcmFndWUxDDAKBgNVBAoM" +
            "A09NRjENMAsGA1UECwwEWm93ZTEQMA4GA1UEAwwHQVBJTVRTVDCCASIwDQYJKoZI" +
            "hvcNAQEBBQADggEPADCCAQoCggEBAJ6L+6l6mfxByy/VrHQ881xkW/GWQQndocPH" +
            "i5Em15P+/ZQToYBTfLPUqGXcPnILg+PrjMtTHBCHO03pIuJxFXqrWfsaxR/O7zhp" +
            "BSTt+iT6/kMBhPdF4sJF2VQo1sGBa79hIn3StvD3hKba/5Rzx8i+WXpKNeCzYRoZ" +
            "BLYH/MLAokgabf0iWjzrwy9STBvZ0uPON4iBhz6bYh0wTra90j0dDjsetTBMOrm9" +
            "gO/sj7RD2KBQUM+mMiny5w4AWjvDChfzGEc37f/Ur2FyCqwY7k4oNS2tMtPQKemg" +
            "4CtmFsWLL3Vb7e6fwoCNFLsmJumsd13u2HCmnV5YT13ZL8xphqkCAwEAAaOBkDCB" +
            "jTALBgNVHQ8EBAMCBeAwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMB8G" +
            "A1UdEQQYMBaCCWxvY2FsaG9zdIIJMTI3LjAuMC4xMB0GA1UdDgQWBBQ3GrkUuyvH" +
            "QmPRECqdzcR3qmQSHzAfBgNVHSMEGDAWgBT78hIus4SCXxMW8T9T0AEIe7HZNjAN" +
            "BgkqhkiG9w0BAQsFAAOCAQEAHAzeBownnYY9kSF6fif+dXw2miRTNkhRRc6ZIlij" +
            "Jy+d5ZysrR0yUTeW11raltGiX2gcCtg5GZp+ODgiqSMJN3mV1bIpKiuBhODKHlMz" +
            "pg8v4ebjIHd1buO8KbOlR8zKv4kMFiGqdfWW6W3BZy3w3RCOnWhts2Y4O+XZ4Gri" +
            "Yjiwkwf1IY7xv7HBJ4BsbUwxjxMcxa1HNqE8oAqEtiFxRmPkAi+g1lijvF26AKZd" +
            "WxKFTLJV1HxUsa5l8b7cHN9yya6IVixVcB9Cla06Rg7dkaI4Deb5JCxFXjoznDKY" +
            "kv8ZumkzQI9Ov90d1FYyVr7VWPEun/XV2XmH9nGHWyJSkA==";

        @MockitoBean
        private ApimlTomcatCustomizer apimlTomcatCustomizer;
        @MockitoBean
        private ApimlInstanceRegistry apimlInstanceRegistry;
        @Mock
        private AttlsContext attlsContext;
        @Mock
        private ThreadLocal<AttlsContext> threadLocal;

        @Test
        void whenFullyStartedUp_thenEmitMessage(CapturedOutput output) throws IoctlCallException, UnknownEnumValueException {
            // Prevent use of native code
            when(apimlInstanceRegistry.getApplications()).thenReturn(applicationRegistry.getApplications());
            doNothing().when(apimlTomcatCustomizer).customize(any());
            ReflectionTestUtils.setField(InboundAttls.class, "contexts", threadLocal);
            lenient().when(threadLocal.get()).thenReturn(attlsContext);
            lenient().when(attlsContext.getCertificate()).thenReturn(Base64.getDecoder().decode(CERTIFICATE));
            lenient().when(attlsContext.getStatConn()).thenReturn(StatConn.SECURE);

            verifyStartupMessage(output);
        }
    }
}
